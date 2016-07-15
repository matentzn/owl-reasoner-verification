package owl.cs.man.ac.uk.justification.verification;
import java.io.File;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.experiment.ExperimentRunner;

public class JustificationGenerationExperimentRunner extends ExperimentRunner{

	public static void main(String[] args) {
		ExperimentRunner runner = new JustificationGenerationExperimentRunner();
		Experiment experiment = runner.configureExperiment(args);
		runner.runExperiment(experiment);
	}
	@Override
	protected Experiment prepare(String[] args) {
		if (args.length != 9) {
			throw new RuntimeException(
					"You need exactly nine parameters: path to ontology, path to experiment csv, path to class hierachies, "
					+ "path to disagreements, reasoner, timeout, just limit, justanalysis, dir for justs.");
		}
		String ontology_path =args[0];
		String csv_path = args[1];
		String classhierarchydir_path = args[2];
		String disagreements = args[3];
		String reasoner = args[4];
		String timeout = args[5];
		String limit = args[6];
		String just_analysis = args[7];
		String justdir = args[8];
		
		this.setCSVFile(new File(csv_path));
		this.setOntologyFile(new File(ontology_path));
		File chdir = new File(classhierarchydir_path);
		File dis = new File(disagreements);
		int to = Integer.valueOf(timeout);
		int lim = Integer.valueOf(limit);
		int ana = Integer.valueOf(just_analysis);
		File jd = new File(justdir);
		
		return new JustificationGenerationExperiment(getOntologyFile(),getCSVFile(),chdir,dis,reasoner,to,lim,ana,jd);
	}

}
