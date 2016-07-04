package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.ore.wrappers.OREv2ReasonerWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;
import owl.cs.man.ac.uk.experiment.experiment.ReasonerExperiment;

public class GenerateInfClassHierarchyExperiment extends ReasonerExperiment {

	private File inferred_hierarchy;

	public GenerateInfClassHierarchyExperiment(File ontfile, File csvfile, File inferred_hierachy, String reasonername,
			int reasoner_timeout) {
		super(ontfile, csvfile, inferred_hierachy, reasonername, reasoner_timeout);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void process() throws Exception {
		// TODO Auto-generated method stub
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		long startload = System.nanoTime();
		OWLOntology o = manager.loadOntologyFromOntologyDocument(getOntologyFile());
		long endload = System.nanoTime();
		OREv2ReasonerWrapper reasonerWrapper = new OREv2ReasonerWrapper(o, getReasonerName(), "error_log_" + getReasonerName() + "_" + System.currentTimeMillis());
		Set<OWLAxiom> inf_ch = reasonerWrapper.classify();
		ClassHierarchyNormaliser chn = new ClassHierarchyNormaliser();
		OWLOntology normalised = chn.loadClassificationResultDataIntoOntology(manager.createOntology(inf_ch));
		Set<OWLAxiom> results = new HashSet<OWLAxiom>();
		for(OWLAxiom ax:normalised.getAxioms())
		{
			if(ax.isOfType(AxiomType.SUBCLASS_OF) || ax.isOfType(AxiomType.EQUIVALENT_CLASSES))
			{
				results.add(ax);
			}
		}
		reasonerWrapper.serializeOntologyResults(results, manager, inferred_hierarchy.getAbsolutePath());
	}

	@Override
	protected Version getExperimentVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void exportInferredHierarchy(OWLOntologyManager manager, String prefix, OWLReasoner r, OWLOntology o)
			throws OWLOntologyCreationException, FileNotFoundException,
			OWLOntologyStorageException {
		if (isExportInferredHierarchy()) {
			File infhier = new File(getInferredHierachyDir(), prefix
					+ getOntologyFile().getName());
			OWLOntology inf = OntologyClassification.getInferredHierarchy(manager, r, o);
			OntologySerialiser.saveOWLXML(infhier.getParentFile(), inf,
					infhier.getName(), manager);
		}
	}
}
