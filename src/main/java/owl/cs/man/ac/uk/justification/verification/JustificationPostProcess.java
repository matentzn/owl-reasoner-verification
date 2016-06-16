package owl.cs.man.ac.uk.justification.verification;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.ontology.OntologyFileNameFilter;

public class JustificationPostProcess {

	public static void main(String[] args) throws FileNotFoundException,
			OWLOntologyCreationException {
		File odir = new File("D:\\just2\\owlxml");
		File justdir = new File("D:\\just2\\wodom_just_data");
		Set<OWLSubClassOfAxiom> certainentailments = new HashSet<OWLSubClassOfAxiom>();
		List<Map<String, String>> justdata = CSVUtilities
				.getAllRecords(new File(justdir, "metadata.csv"));
		for (File ontology : odir.listFiles(new OntologyFileNameFilter())) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology o = man.loadOntologyFromOntologyDocument(ontology);
			Set<OWLAxiom> axo = getAxiomsWOAnnotation(o.getLogicalAxioms());
			System.out.println("##########");
			System.out.println(ontology.getName());
			File justdiro = new File(justdir, ontology.getName());
			for (File entailmentf : justdiro.listFiles()) {
				for (File reasonerf : entailmentf.listFiles()) {
					for (File justf : reasonerf
							.listFiles(new OntologyFileNameFilter())) {
						List<Map<String, String>> jusdat = getEntries(
								justf.getName(), justdata);
						OWLClass subClass = OWLManager.getOWLDataFactory()
								.getOWLClass(
										IRI.create(jusdat.get(0)
												.get("subclass")));
						OWLClass superClass = OWLManager.getOWLDataFactory()
								.getOWLClass(
										IRI.create(jusdat.get(0).get(
												"superclass")));

						System.out.println(justf.getName());
						OWLOntologyManager man2 = OWLManager
								.createOWLOntologyManager();
						OWLOntology o2 = man2
								.loadOntologyFromOntologyDocument(justf);
						OWLSubClassOfAxiom sax = man2.getOWLDataFactory()
								.getOWLSubClassOfAxiom(subClass, superClass);
						//Set<OWLAxiom> axjust = getAxiomsWOAnnotation(o2
							//	.getLogicalAxioms());
						boolean asserted = axo.contains(sax);
						if (asserted)
							certainentailments.add(sax);
						
						for (Map<String, String> d : justdata) {
							if (d.get("justname").equals(justf.getName())) {
								d.put("asserted_entailment", asserted + "");
								d.put("filepath", justf.getAbsolutePath());
							}
						}
					}
				}
			}
			for (Map<String, String> d : justdata) {
				if(!d.get("ontology").equals(ontology.getName())){
					continue;
				}
				System.out.println(d);
				OWLOntologyManager man2 = OWLManager.createOWLOntologyManager();
				OWLOntology o2 = man2
						.loadOntologyFromOntologyDocument(new File(d
								.get("filepath")));
				Set<OWLAxiom> axjust = getAxiomsWOAnnotation(o2
						.getLogicalAxioms());
				int sizebefore = axjust.size();
				axjust.removeAll(certainentailments);
				int sizeafter = axjust.size();
				d.put("depent_on_asserted", (sizeafter!=sizebefore) + "");
			}
		}
		CSVUtilities.writeCSVData(new File(justdir,"metadata_postpro.csv"), justdata, false);
	}

	private static Set<OWLAxiom> getAxiomsWOAnnotation(
			Set<OWLLogicalAxiom> axioms) {
		Set<OWLAxiom> axo = new HashSet<OWLAxiom>();
		for (OWLAxiom ax : axioms) {
			axo.add(ax.getAxiomWithoutAnnotations());
		}
		return axo;
	}

	private static List<Map<String, String>> getEntries(String name,
			List<Map<String, String>> justdata) {
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		for (Map<String, String> d : justdata) {
			if (d.get("justname").equals(name)) {
				data.add(d);
			}
		}
		return data;
	}

}
