package owl.cs.man.ac.uk.justification.verification;


import java.io.File;
import java.io.FileNotFoundException;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;

public class JustificationTimeExperimentRunner extends ExperimentRunner {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) throws FileNotFoundException {
		ExperimentRunner runner = new JustificationTimeExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}

	@Override
	protected Experiment prepare(String[] args) {
		if (args.length != 9) {
			throw new RuntimeException(
					"You need exactly five parameters: path to ontology, reasoner, path to csv, reasoner timeout, path to output dir for inferred hierachy.");
		}

		String ontology_path = args[0];
		String csv_path = args[1];
		String inf1_path = args[2];
		String r1 = args[3];
		String timeout = args[4];
		String just_dir = args[5];
		String limit_str = args[6];
		String limitanal_str = args[7];
		String no_inf = args[8];
		
		int reasoner_timeout = Integer.valueOf(timeout);
		int limit_just = Integer.valueOf(limit_str);
		int limit_analysis = Integer.valueOf(limitanal_str);
		setProcessTimeout(90000 + (reasoner_timeout));
		boolean dont_compute_inf = no_inf.equals("true");
		
		setCSVFile(new File(csv_path));
		setOntologyFile(new File(ontology_path));
		File just_out =just_dir.isEmpty() ? null : new File(just_dir);
		File inf1 = new File(inf1_path);	
		
		return new JustificationTimeExperiment(getOntologyFile(), getCSVFile(), just_out, r1, inf1, reasoner_timeout,limit_just,limit_analysis, dont_compute_inf);
	}

}
