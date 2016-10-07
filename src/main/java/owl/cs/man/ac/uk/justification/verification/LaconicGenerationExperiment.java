package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationManager;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;
import owl.cs.man.ac.uk.experiment.experiment.ReasonerExperiment;
import owl.cs.man.ac.uk.experiment.ontology.MetricsLabels;
import owl.cs.man.ac.uk.experiment.util.ExperimentUtilities;
import owl.cs.man.ac.uk.experiment.util.ReasonerUtilities;

public class LaconicGenerationExperiment extends ReasonerExperiment {
	private int just_limit;
	private int limit_analysis;
	private File just_out;
	private List<Map<String,String>> just_data = new ArrayList<Map<String,String>>();
	private List<Map<String,String>> ent_data = new ArrayList<Map<String,String>>();
	private List<Map<String,String>> ent_no_gen_data = new ArrayList<Map<String,String>>();
	private File csv_just;
	private File csv_ent;
	File csv_no_gen;
	public static IRI ENTAILMENT_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#entailment");
	public static IRI REASONER_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#reasoner");
	public static IRI REASONERJAR_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#reasoner_version");
	public static IRI GENJUSTIFICATION_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#generating_justification");
	public static IRI JUSTIFICATION_NAME_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#justification_name");
	public LaconicGenerationExperiment(File justification, File csv, File csv_just, File csv_ent, File csv_no_gen, File inf_hir, String reasoner,int timeout,int just_limit,int limit_analysis,File justPath) {
		super(justification, csv, inf_hir, reasoner,timeout);
		this.just_limit = just_limit;
		this.limit_analysis = limit_analysis;
		this.just_out = justPath;
		this.csv_just = csv_just;
		this.csv_ent = csv_ent;
		this.csv_no_gen = csv_no_gen;
	}

	@Override
	protected void process() throws Exception {
		//initialisation
		System.out.println("Reasoner: " + getReasonerName());
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		long startload = System.nanoTime();
		OWLOntology o = manager.loadOntologyFromOntologyDocument(getOntologyFile());
		long endload = System.nanoTime();
		OWLDataFactory df = manager.getOWLDataFactory();
		OWLAnnotationProperty reasonerAnnotationProperty = df.getOWLAnnotationProperty(REASONER_ANNOTATION_PROPERTY_IRI);
		OWLAnnotation reasonerAnnotation = df.getOWLAnnotation(reasonerAnnotationProperty, df.getOWLLiteral(getReasonerName()));
		OWLAnnotationProperty genjustificationAnnotationProperty = df.getOWLAnnotationProperty(GENJUSTIFICATION_ANNOTATION_PROPERTY_IRI);
		OWLAnnotation genjustificationAnnotation = df.getOWLAnnotation(genjustificationAnnotationProperty, df.getOWLLiteral(getOntologyFile().getAbsolutePath()));
		OWLAnnotationProperty justificationAnnotationProperty = df.getOWLAnnotationProperty(JUSTIFICATION_NAME_ANNOTATION_PROPERTY_IRI);
		OWLAnnotation justificationAnnotation = df.getOWLAnnotation(justificationAnnotationProperty, df.getOWLLiteral(getOntologyFile().getName()));
		OWLAnnotationProperty reasonerVerAnnotationProperty = df.getOWLAnnotationProperty(REASONERJAR_ANNOTATION_PROPERTY_IRI);
		long reasonerinit = System.nanoTime();
		OWLReasonerFactory df_just = ReasonerUtilities.getFactory(this.getReasonerName());
		ExplanationGeneratorFactory<OWLAxiom> genFac = ExplanationManager.createLaconicExplanationGeneratorFactory(df_just);
		// Now create the actual explanation generator for our ontology
		ExplanationGenerator<OWLAxiom> gen = genFac
				.createExplanationGenerator(o);
		long reasonerend = System.nanoTime();
		OWLAnnotation reasonerVerAnnotation = df.getOWLAnnotation(reasonerVerAnnotationProperty, df.getOWLLiteral(MetricsLabels.REASONER_JAR, ExperimentUtilities.getJARName(getReasonerFactory().getClass())));
		long entailmentstart = System.nanoTime();
		OWLSubClassOfAxiom entailment = this.entailSearch(o, df);
		long entailmentend = System.nanoTime();
		long generationstart = System.nanoTime();
		String entailmentstr = entailment.getSubClass().asOWLClass()
						.getIRI().getRemainder().or("")
						+ "_"
						+ entailment.getSuperClass().asOWLClass().getIRI()
								.getRemainder().or("");

		System.out.println("Computing laconic explanations for entailment: "
				+ entailmentstr);
		Set<Explanation<OWLAxiom>> expl = new HashSet<Explanation<OWLAxiom>>();
			
		try {
			if(just_limit>0) {
				expl.addAll(gen.getExplanations(entailment, just_limit));
				}
			else {
					expl.addAll(gen.getExplanations(entailment));	
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<Explanation<OWLAxiom>> expl_analysis;
			//Entering 0 means we do not have any limit to the
			//justifications we review
			if(limit_analysis != 0){
				expl_analysis = getSmallestExplanations(
				expl, limit_analysis);
			}
			else{
				expl_analysis = new ArrayList<Explanation<OWLAxiom>>(expl);
			}
				
		//Adds annotation for relevant entailment axiom. Based off Sam and Matt's standards
			
		//OWLAnnotationProperty entailmentAnnotationProperty = df.getOWLAnnotationProperty(ENTAILMENT_ANNOTATION_PROPERTY_IRI);
		if(expl_analysis.isEmpty()){
				Map<String,String> no_gen_data = new HashMap<String,String>();
				no_gen_data.put("entailment",entailmentstr);
				no_gen_data.put("reasoner",getReasonerName());
				no_gen_data.put("justification",getOntologyFile().getName());
				ent_no_gen_data.add(no_gen_data);
		 }
		OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();						
		for (Explanation<OWLAxiom> explanation : expl_analysis) {
			Map<String,String> data = new HashMap<String,String>();
			String justname = "laconic_just_" + getOntologyFile().getName() 
			+"_" + getReasonerName() + "_" + explanation.hashCode() +  ".owl";
			Set<OWLAxiom> just = explanation.getAxioms();
			//just.add(entailment);
			//** current unideal method for marking out subclass axioms
			OWLClass subcl = (OWLClass) ((OWLSubClassOfAxiom) explanation.getEntailment()).getSubClass();
			OWLClass supcl = (OWLClass) ((OWLSubClassOfAxiom) explanation.getEntailment()).getSuperClass();
			OWLAnnotation commentSub = df.getOWLAnnotation(df.getRDFSComment(),
			df.getOWLLiteral("This is the subclass of the entailment that follows from this justification.", "en"));
			OWLAnnotation commentSup = df.getOWLAnnotation(df.getRDFSComment(),
			df.getOWLLiteral("This is the superclass of the entailment that follows from this justification.", "en"));
			OWLAxiom ax1 = df.getOWLAnnotationAssertionAxiom((OWLAnnotationSubject) subcl.getIRI(), commentSub);
			OWLAxiom ax2 = df.getOWLAnnotationAssertionAxiom((OWLAnnotationSubject) supcl.getIRI(), commentSup);
			OWLAxiom ax3 = df.getOWLDeclarationAxiom(subcl);
			OWLAxiom ax4 = df.getOWLDeclarationAxiom(supcl);
			//OWLAnnotation entailmentAnnotation = df.getOWLAnnotation(entailmentAnnotationProperty, df.getOWLLiteral(entailment.toString()));
			OWLOntology justo = manager2.createOntology(just);
			manager2.applyChange(new AddAxiom(justo,ax1));
			manager2.applyChange(new AddAxiom(justo,ax2));
			manager2.applyChange(new AddAxiom(justo,ax3));
			manager2.applyChange(new AddAxiom(justo,ax4));
			//manager.applyChange(new AddOntologyAnnotation(justo,entailmentAnnotation));
			manager2.applyChange(new AddOntologyAnnotation(justo,reasonerAnnotation));
			manager2.applyChange(new AddOntologyAnnotation(justo,genjustificationAnnotation));
			manager2.applyChange(new AddOntologyAnnotation(justo,justificationAnnotation));
			manager2.applyChange(new AddOntologyAnnotation(justo,reasonerVerAnnotation));
			OntologySerialiser
					.saveOWLXML(just_out,
								justo,
								justname,
								manager);
					data.put("filename",justname);
					data.put("hashcode", "" + explanation.hashCode());
					data.put("entailment",entailmentstr);
					data.put("reasoner",getReasonerName());
					data.put("justification",getOntologyFile().getName());
					data.put("size","" + just.size());
				just_data.add(data);
		}
		Map<String,String> d = new HashMap<String,String>();
		d.put("entailment",entailment.getAxiomWithoutAnnotations().toString());
		d.put("hashcode","" + entailment.hashCode());
		d.put("justification",getOntologyFile().getName());
		d.put("just_count","" + expl_analysis.size());
		ent_data.add(d);
		
		long generationend = System.nanoTime();
		//Data gathering
		addResult("ontology_load_time", "" + (endload - startload));
		addResult("reasoner_load_time","" + (reasonerend - reasonerinit));
		addResult("entailment_find_time","" + (entailmentend - entailmentstart));
		addResult("generation_time",""+(generationend - generationstart));
		CSVUtilities.writeCSVData(csv_just, just_data, true);
		CSVUtilities.writeCSVData(csv_ent, ent_data, true);
		CSVUtilities.writeCSVData(csv_no_gen, ent_no_gen_data, true);
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1,0,0,1);
	}
		
	
	//load Ontology & produce disagreement axioms as set
	//failure produces an error flag and an empty set.
	/**
	private Set<OWLSubClassOfAxiom> renderDisagreement(File disagreements){
		OWLOntologyManager ontoman = OWLManager.createOWLOntologyManager();
		Set<OWLSubClassOfAxiom> returnSet = new HashSet<OWLSubClassOfAxiom>();
		try {
			OWLOntology ontology = ontoman.loadOntologyFromOntologyDocument(disagreements);
			for(OWLAxiom ax:ontology.getLogicalAxioms())
			{	
				if(ax.isOfType(AxiomType.SUBCLASS_OF))
				{
					returnSet.add((OWLSubClassOfAxiom) ax);
				}
			}
		} catch (OWLOntologyCreationException e) {
			System.out.println("Failed to load disagreement Ontology file: " + disagreements.getAbsolutePath());
			e.printStackTrace();
		}
		return returnSet;
	}
	**/
	//Takes the limit of the analysis and returns the smallest
	//of the justifications up to that limit
	private List<Explanation<OWLAxiom>> getSmallestExplanations(
			Set<Explanation<OWLAxiom>> expl, int limit) {
		List<Explanation<OWLAxiom>> expl_small = new ArrayList<Explanation<OWLAxiom>>();
		Map<Integer, Set<Explanation<OWLAxiom>>> ex_map = new HashMap<Integer, Set<Explanation<OWLAxiom>>>();
		for (Explanation<OWLAxiom> e : expl) {
			Integer size = e.getSize();
			if (!ex_map.containsKey(size)) {
				ex_map.put(size, new HashSet<Explanation<OWLAxiom>>());
			}
			ex_map.get(size).add(e);
		}
		List<Integer> sizes = new ArrayList<Integer>(ex_map.keySet());
		Collections.sort(sizes);
		for (Integer size : sizes) {
			expl_small.addAll(ex_map.get(size));
		}
		if (limit < expl_small.size()) {
			return expl_small.subList(0, limit);
		} else {
			return expl_small;
		}

	}
	
	private OWLSubClassOfAxiom entailSearch(OWLOntology just, OWLDataFactory df) throws OWLOntologyCreationException{
		Set<OWLClass> anjuClass = just.getClassesInSignature();
		anjuClass.add(df.getOWLThing());
		anjuClass.add(df.getOWLNothing());
		OWLClass subclass = null;
		OWLClass supclass = null;
		Boolean cond1 = false;
		Boolean cond2 = false;
		for(OWLClass cl:anjuClass)
		{
			Set<OWLAnnotationAssertionAxiom> anno = cl.getAnnotationAssertionAxioms(just);
			if(!anno.isEmpty())
			{
				for(OWLAnnotationAssertionAxiom aax : anno)
				{
					if(aax.getAnnotation().getValue().toString().contains("superclass") && !cond1)
					{
							cond1 = true;
							supclass = cl;
					}
					else if(aax.getAnnotation().getValue().toString().contains("subclass") && !cond2)
					{
							cond2 = true;
							subclass = cl;
					}
				}
				
			}
		}
		System.out.println(cond1);
		System.out.println(cond2);
		System.out.println(subclass);
		System.out.println(supclass);
		if(cond1 && cond2)
		{
			return df.getOWLSubClassOfAxiom(subclass, supclass);
		}
		else 
		{
			return null;
		}
		
	}

}