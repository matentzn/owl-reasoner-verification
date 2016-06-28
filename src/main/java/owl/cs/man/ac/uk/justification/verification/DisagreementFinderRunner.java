package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class DisagreementFinderRunner {

	public static void main(String[] args) {
		DisagreementFinder disf = new DisagreementFinder();
		ArrayList<String> paths = new ArrayList<String>();
		String path = "/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/cao_wo_taut.disagreements.owl";
		paths.add("/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/inf_fact_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/inf_hermit_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/inf_jfact_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/inf_more-hermit_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/disagreement_finder/disagreement_with_sj_cao/inf_pellet_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		OWLOntologyManager ontom = OWLManager.createOWLOntologyManager();
		OWLOntology ontology;
		try {
			ontology = ontom.loadOntologyFromOntologyDocument(new File("/home/michael/Downloads/website_iswc2015/ontologies_w_problems/cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl"));
			ArrayList<String> list = disf.getDifferences(paths, path,ontology);
			for(String s:list)
			{
				System.out.println(s);
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
