package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;
import owl.cs.man.ac.uk.experiment.experiment.ReasonerExperiment;

public class GenerateInfClassHierarchyExperiment extends ReasonerExperiment {

	private String approach;
	public GenerateInfClassHierarchyExperiment(File ontfile, File csvfile, File inferred_hierachy, String reasonername,
			int reasoner_timeout, String approach) {
		super(ontfile, csvfile, inferred_hierachy, reasonername, reasoner_timeout);
		// TODO Auto-generated constructor stub
		this.approach = approach;
	}

	@Override
	protected void process() throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Reasoner: " + getReasonerName());
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		long startload = System.nanoTime();
		OWLOntology o = manager.loadOntologyFromOntologyDocument(getOntologyFile());
		long endload = System.nanoTime();
		long start = System.currentTimeMillis();
		System.out.println("Classification...");
		Boolean consistent = true;
		OWLReasoner reasoner = createReasoner(o);
		try {
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			System.out.println("...finished classification");
			consistent = reasoner.isConsistent();
		} catch (InconsistentOntologyException e) {		
			e.printStackTrace();
			consistent = false;
		} 		
		long end = System.currentTimeMillis();
		//Data collection
		System.out.println("Exporting...");
		exportInferredHierarchy(manager, reasoner, o);
		System.out.println("...finished exporting.");
		addResult("normaliser", "" + approach);
		addResult("ontology_loading_time", "" + (endload - startload));
		addResult("classification_time", "" + (end - start));
		addResult("consistent","" + consistent);
		addResult("asserted_size","" + o.getAxiomCount());
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}

	@Override
	protected void exportInferredHierarchy(OWLOntologyManager manager, String prefix, OWLReasoner r, OWLOntology o)
			throws OWLOntologyCreationException, FileNotFoundException,
			OWLOntologyStorageException {
		if (isExportInferredHierarchy()) {
			OWLOntology out;
			File infhier = new File(getInferredHierachyDir(), prefix
					+ getOntologyFile().getName());
			Set<OWLAxiom> resultAxioms = new HashSet<OWLAxiom>();
			System.out.println("Normalising with method " + approach + "...");
			long normstart = System.currentTimeMillis();
			if(!r.isConsistent()) {
				OWLDataFactory factory = manager.getOWLDataFactory();
				resultAxioms.add(factory.getOWLSubClassOfAxiom(factory.getOWLThing(), factory.getOWLNothing()));
				out = OWLManager.createOWLOntologyManager().createOntology(resultAxioms);
				
			}
			else if (approach.equals("ore")) {
				InferredSubClassAxiomGenerator subClassGenerator = new InferredSubClassAxiomGenerator();
				InferredEquivalentClassAxiomGenerator equivClassGenerator = new InferredEquivalentClassAxiomGenerator();
				Set<OWLSubClassOfAxiom> subClassAxioms = subClassGenerator.createAxioms(manager, r);
				Set<OWLEquivalentClassesAxiom> equivClassAxioms = equivClassGenerator.createAxioms(manager, r);
				resultAxioms.addAll(subClassAxioms);
				resultAxioms.addAll(equivClassAxioms);
				OWLOntology inf = OWLManager.createOWLOntologyManager().createOntology(resultAxioms);
				ClassHierarchyNormaliser chn = new ClassHierarchyNormaliser();
				out = chn.loadClassificationResultDataIntoOntology(inf);
			}
			else if (approach.equals("bails")) {
				out = OntologyClassification.getInferredHierarchy(manager, r, o);
			}
			else {
				return;
			}
			long normend = System.currentTimeMillis();
			System.out.println("...finished normalisation.");
			addResult("normalisation_time", "" + (normend - normstart));
			addResult("inferred_size","" + resultAxioms.size());
			OntologySerialiser.saveOWLXML(infhier.getParentFile(), out,
					infhier.getName(), manager);
		}
	}
}
