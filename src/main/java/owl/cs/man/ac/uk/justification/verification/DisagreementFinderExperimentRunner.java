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
		if (args.length != 6) {
			throw new RuntimeException(
					"You need exactly four parameters: path to ontology, path to experiment csv, path to class hierachies, approach to normalisation.");
		}
		String ontology_path = args[0];
		String csv_path = args[1];
		String classhierarchydir_path = args[2];
		String out_dir = args[3];
		String approach = args[4];
		String to = args[5];
		
		this.setCSVFile(new File(csv_path));
		this.setOntologyFile(new File(ontology_path));
		long reasoner_time = Long.valueOf(to);
		setProcessTimeout(60000 + (reasoner_time));
		File classhierarchydir = new File(classhierarchydir_path);
		File diagreementoutdir = new File(out_dir,"out");
		File sj_dir = new File(diagreementoutdir.getParentFile(),"sj_out");
		diagreementoutdir.mkdir();
		sj_dir.mkdir();
		return new DisgreementFinderExperiment(getOntologyFile(), getCSVFile(), classhierarchydir, sj_dir, diagreementoutdir, approach, reasoner_time);
	}

}
