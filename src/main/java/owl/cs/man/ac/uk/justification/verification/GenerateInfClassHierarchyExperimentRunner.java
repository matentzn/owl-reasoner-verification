package owl.cs.man.ac.uk.justification.verification;

import java.io.File;

import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;

public  class GenerateInfClassHierarchyExperimentRunner extends ExperimentRunner{

	public static void main(String[] args) {
		ExperimentRunner runner = new GenerateInfClassHierarchyExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}

	protected Experiment prepare(String[] args) {
		if (args.length != 6) {
			throw new RuntimeException(
					"You need exactly six parameters: path to ontology, path to experiment csv, path to class hierachies, approach to normalisation.");
		}
		String ontology_path = args[0];
		String csv_path = args[1];
		String inf_dir_path = args[2];
		String reasonername = args[3];
		String reasoner_timeout = args[4];
		String approach = args[5];
		
		int reasoner_time = Integer.valueOf(reasoner_timeout);
		setProcessTimeout(5000 + (reasoner_time));
		setCSVFile(new File(csv_path));
		setOntologyFile(new File(ontology_path));
		File inf_dir = new File(inf_dir_path);
		
		return new GenerateInfClassHierarchyExperiment(getOntologyFile(), getCSVFile(), inf_dir, reasonername, reasoner_time, approach);
	}

}
