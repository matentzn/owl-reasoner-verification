package owl.cs.man.ac.uk.justification.verification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class DisagreementFinderRunner {

	public static void main(String[] args) {
		DisagreementFinder disf = new DisagreementFinder();
		ArrayList<String> paths = new ArrayList<String>();
		String path = "/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/cao.disagreements.owl";
		paths.add("/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/inf_fact_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/inf_hermit_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/inf_jfact_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/inf_more-hermit_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		paths.add("/home/michael/experiments/spot_tests/reasoner_verification_test/test_3/inf_pellet_cao.clusters-of-orthologous-groups-cog-analysis-ontology.3.orig.owl");
		disf.getDifferences(paths, path);
	}
	
	
}
