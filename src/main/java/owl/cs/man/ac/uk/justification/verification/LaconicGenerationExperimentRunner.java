import java.io.File;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;
import owl.cs.man.ac.uk.justification.verification.LaconicGenerationExperiment;

public class LaconicGenerationExperimentRunner extends ExperimentRunner {

	public static void main(String[] args) {
		ExperimentRunner runner = new LaconicGenerationExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}
	@Override
	protected Experiment prepare(String[] args) {
		if (args.length !=11) {
			throw new RuntimeException(
					"You need exactly eight parameters: path to ontology, path to experiment csv, path to class hierachies, "
					+ "reasoner, timeout, just limit, justanalysis, dir for justs.");
		}
		String ontology_path =args[0];
		String csv_path = args[1];
		String csv_just_path = args[2];
		String csv_ent_path = args[3];
		String csv_no_gen_path = args[4];
		String classhierarchydir_path = args[5];
		String reasoner = args[6];
		String timeout = args[7];
		String limit = args[8];
		String just_analysis = args[9];
		String justdir = args[10];
		
		this.setCSVFile(new File(csv_path));
		this.setOntologyFile(new File(ontology_path));
		File chdir = new File(classhierarchydir_path);
		File csv_just = new File(csv_just_path);
		File csv_ent = new File(csv_ent_path);
		File csv_no_gen = new File(csv_no_gen_path);
		int to = Integer.valueOf(timeout);
		int lim = Integer.valueOf(limit);
		int ana = Integer.valueOf(just_analysis);
		File jd = new File(justdir);
		
		return new LaconicGenerationExperiment(getOntologyFile(),getCSVFile(),csv_just,csv_ent,csv_no_gen,chdir,reasoner,to,lim,ana,jd);
	}
}
