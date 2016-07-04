package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;

public class DisagreementExperiment extends Experiment {

	String ontologyDir;
	String classhierachyDir;
	ArrayList<String> classhierachyList;
	OWLOntology ontology;
	DisagreementFinder df;
	
	public DisagreementExperiment(ArrayList<String> classhierachyList,
			String classhierachyDir,
			File ontfile,
			File csvfile){
		super(ontfile,csvfile);
		this.classhierachyDir = classhierachyDir;
		this.classhierachyList = classhierachyList;
		this.df = new DisagreementFinder();
		OWLOntologyManager ontoman = OWLManager.createOWLOntologyManager();
		try {
			this.ontology = ontoman.loadOntologyFromOntologyDocument(getOntologyFile());
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void process() throws Exception {
			
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}
	
}
