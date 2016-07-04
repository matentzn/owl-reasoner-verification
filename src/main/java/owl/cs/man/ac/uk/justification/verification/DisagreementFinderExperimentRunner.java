package owl.cs.man.ac.uk.justification.verification;

import java.io.File;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;

public class DisagreementFinderExperimentRunner extends ExperimentRunner {

	public static void main(String[] args) {
		ExperimentRunner runner = new DisagreementFinderExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}

	@Override
	protected Experiment prepare(String[] args) {
		if (args.length != 3) {
			throw new RuntimeException(
					"You need exactly three parameters: path to ontology, path to experiment csv, path to class hierachies");
		}
		String ontology_path = args[0];
		String csv_path = args[1];
		String classhierarchydir_path = args[2];
		
		this.setCSVFile(new File(csv_path));
		this.setOntologyFile(new File(ontology_path));
		File sj_csv = new File(getCSVFile().getParentFile(),getOntologyFile().getName()+"_sj_verdicts.csv");
		sj_csv.setWritable(true);
		File classhierarchydir = new File(classhierarchydir_path);
		File diagreementoutdir = new File(getCSVFile().getParentFile(),"out");
		diagreementoutdir.mkdir();

		return new DisgreementFinderExperiment(getOntologyFile(), getCSVFile(), classhierarchydir, sj_csv, diagreementoutdir);
	}

}
