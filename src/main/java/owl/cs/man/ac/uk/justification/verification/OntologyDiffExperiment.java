package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.ontology.CompareOntologies;

public class OntologyDiffExperiment extends Experiment {
	private File ontfile1;
	private File exportDir;
	private String compid;

	public OntologyDiffExperiment(File original, String savePath, File ontfile1, String compid) {
		super(original, new File(savePath));
		this.ontfile1 = ontfile1;
		this.exportDir = new File(savePath);
		this.compid = compid;
	}

	@Override
	protected void process() throws Exception {
		addResult(CompareOntologies.compareOntologies(getOntologyFile(), ontfile1, "o1", "o2", compid, exportDir));
	}
	

	@Override
	protected Version getExperimentVersion() {
		return new Version(1,0,0,1);
	}

}
