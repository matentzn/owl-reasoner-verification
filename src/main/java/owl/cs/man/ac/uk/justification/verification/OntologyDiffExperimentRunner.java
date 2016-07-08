package owl.cs.man.ac.uk.justification.verification;

import java.io.File;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;

public class OntologyDiffExperimentRunner extends ExperimentRunner{

	public static void main(String[] args) {
		ExperimentRunner runner = new OntologyDiffExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}

	@Override
	protected Experiment prepare(String[] args) {
		if (args.length != 4) {
			throw new RuntimeException(
					"You need exactly seven parameters: path to original ontology, path to data and csv dir, path to 2 comparison ontologies, 2 reference names for comparison ontologies"
					+ " compid.");
		}
		String ontology_path = args[0];
		String csv_path = args[1];
		String ontology_comp1_path = args[2];
		String compid = args[3];
		File ontology = new File(ontology_path);
		File ontology_comp_1 = new File(ontology_comp1_path);
		this.setCSVFile(new File(csv_path));
		this.setOntologyFile(new File(ontology_path));
		return new OntologyDiffExperiment(ontology, csv_path, ontology_comp_1, compid);
	}

}
