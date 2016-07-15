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
import org.semanticweb.owlapi.model.OWLClassExpression;
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

public class JustificationGenerationExperiment extends ReasonerExperiment{
	private int just_limit;
	private int limit_analysis;
	private File just_out;
	private File disagreements;
	private List<Map<String,String>> just_data = new ArrayList<Map<String,String>>();
	private File csv_just;
	public static IRI ENTAILMENT_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#entailment");
	public static IRI REASONER_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#reasoner");
	public static IRI REASONERJAR_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#reasoner_version");
	public static IRI GENONTOLOGY_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#generating_ontology");
	public static IRI NAME_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#ontology_name");
	public JustificationGenerationExperiment(File ontology, File csv, File inf_hir, File disagreements, String reasoner,int timeout,int just_limit,int limit_analysis,File justPath) {
		super(ontology, csv, inf_hir, reasoner,timeout);
		this.just_limit = just_limit;
		this.limit_analysis = limit_analysis;
		this.just_out = justPath;
		this.disagreements = disagreements;
		this.csv_just = new File(csv.getParentFile(),"just_"+reasoner+"_"+ontology.getName()+"_"+csv.getName());
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
		OWLAnnotationProperty genontologyAnnotationProperty = df.getOWLAnnotationProperty(GENONTOLOGY_ANNOTATION_PROPERTY_IRI);
		OWLAnnotation genontologyAnnotation = df.getOWLAnnotation(genontologyAnnotationProperty, df.getOWLLiteral(getOntologyFile().getAbsolutePath()));
		OWLAnnotationProperty ontologyAnnotationProperty = df.getOWLAnnotationProperty(NAME_ANNOTATION_PROPERTY_IRI);
		OWLAnnotation ontologyAnnotation = df.getOWLAnnotation(ontologyAnnotationProperty, df.getOWLLiteral(getOntologyFile().getName()));
		OWLAnnotationProperty reasonerVerAnnotationProperty = df.getOWLAnnotationProperty(REASONERJAR_ANNOTATION_PROPERTY_IRI);
		long reasonerinit = System.nanoTime();
		OWLReasonerFactory df_just = ReasonerUtilities.getFactory(this.getReasonerName());
		ExplanationGeneratorFactory<OWLAxiom> genFac = ExplanationManager
				.createExplanationGeneratorFactory(df_just);
		// Now create the actual explanation generator for our ontology
		ExplanationGenerator<OWLAxiom> gen = genFac
				.createExplanationGenerator(o);
		long reasonerend = System.nanoTime();
		OWLAnnotation reasonerVerAnnotation = df.getOWLAnnotation(reasonerVerAnnotationProperty, df.getOWLLiteral(MetricsLabels.REASONER_JAR, ExperimentUtilities.getJARName(getReasonerFactory().getClass())));
		long disagreementrender = System.nanoTime();
		Set<OWLSubClassOfAxiom> disagreeSet = this.renderDisagreement(disagreements);
		long disagreementend = System.nanoTime();
		long generationstart = System.nanoTime();
		for(OWLSubClassOfAxiom entailment:disagreeSet)
		{
				String entailmentstr = entailment.getSubClass().asOWLClass()
						.getIRI().getRemainder().or("")
						+ "_"
						+ entailment.getSuperClass().asOWLClass().getIRI()
								.getRemainder().or("");

				System.out.println("Computing explanations for entailment: "
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
				
				for (Explanation<OWLAxiom> explanation : expl_analysis) {
					Map<String,String> data = new HashMap<String,String>();
					String justname = "just_" + getOntologyFile().getName() 
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
					//OWLAnnotation entailmentAnnotation = df.getOWLAnnotation(entailmentAnnotationProperty, df.getOWLLiteral(entailment.toString()));
					OWLOntology justo = manager.createOntology(just);
					manager.applyChange(new AddAxiom(justo,ax1));
					manager.applyChange(new AddAxiom(justo,ax2));
					//manager.applyChange(new AddOntologyAnnotation(justo,entailmentAnnotation));
					manager.applyChange(new AddOntologyAnnotation(justo,reasonerAnnotation));
					manager.applyChange(new AddOntologyAnnotation(justo,genontologyAnnotation));
					manager.applyChange(new AddOntologyAnnotation(justo,ontologyAnnotation));
					manager.applyChange(new AddOntologyAnnotation(justo,reasonerVerAnnotation));
					OntologySerialiser
					.saveOWLXML(just_out,
								justo,
								justname,
								manager);
					data.put("filename",justname);
					data.put("hashcode", "" + explanation.hashCode());
					data.put("entailment",entailmentstr);
					data.put("reasoner",getReasonerName());
					data.put("ontology",getOntologyFile().getName());
					data.put("size","" + just.size());
					just_data.add(data);
				}
				
		}
		long generationend = System.nanoTime();
		//Data gathering
		addResult("ontology_load_time", "" + (endload - startload));
		addResult("reasoner_load_time","" + (reasonerend - reasonerinit));
		addResult("disagreement_load_time","" + (disagreementend - disagreementrender));
		addResult("generation_time",""+(generationend - generationstart));
		CSVUtilities.writeCSVData(csv_just, just_data, false);
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1,0,0,1);
	}
		
	
	//load Ontology & produce disagreement axioms as set
	//failure produces an error flag and an empty set.
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

}
