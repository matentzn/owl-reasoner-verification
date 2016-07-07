package owl.cs.man.ac.uk.justification.verification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.experiment.Experiment;

public class DisgreementFinderExperiment extends Experiment {

	private ArrayList<String> classhierachyList = new ArrayList<String>();
	private OWLOntology ontology;
	private OWLOntologyManager ontoman;
	private File disagreementoutdir;
	private File sjcsv;
	private String approach;
	private List<Map<String, String>> csv = new ArrayList<Map<String, String>>();

	public DisgreementFinderExperiment(File ontfile, File csvfile, File classhierarchydirf, File sjcsv,
			File disagreementoutdir, String approach) {
		super(ontfile, csvfile);
		this.ontoman = OWLManager.createOWLOntologyManager();
		this.disagreementoutdir = disagreementoutdir;
		this.sjcsv = sjcsv;
		this.approach = approach;
		try {
			this.ontology = ontoman.loadOntologyFromOntologyDocument(getOntologyFile());
			String oname = getOntologyFile().getName();
			for (File diso : classhierarchydirf.listFiles()) {
				if (diso.getName().contains(oname)) {
					classhierachyList.add(diso.getAbsolutePath());
				}
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void process() throws Exception {
		System.out.println("Process started...");
		long start = System.nanoTime();
		ArrayList<String> selfjustList = this.getDifferences(classhierachyList,
				disagreementoutdir.getAbsolutePath() + "/" + getOntologyFile().getName() + "_disagreements.owl",
				ontology);
		System.out.println("Creating CSV of self-just disagreements.");
		PrintWriter outStr = new PrintWriter(new BufferedWriter(new FileWriter(sjcsv, true)));
		outStr.println("axiom,agree,disagree");
		for (String s : selfjustList) {
			outStr.println(s);
		}
		outStr.close();
		long end = System.nanoTime();
		addResult("disagreement_finder_time", "" + (end - start));
		CSVUtilities.writeCSVData(new File(getCSVFile().getParent(), getOntologyFile().getName() + "_disagreement.csv"),
				csv, false);
	}

	public ArrayList<String> getDifferences(ArrayList<String> fileList, String savePath, OWLOntology original) {
		ArrayList<String> cleanedList = this.cleanList(fileList);
		Set<OWLAxiom> differenceSet = this.getAxioms(new File(cleanedList.get(0)));
		Set<OWLAxiom> saveSet = new HashSet<OWLAxiom>(differenceSet);
		Map<String, Set<OWLAxiom>> collection = new HashMap<String, Set<OWLAxiom>>();
		System.out.println("Constructing disagreement set...");
		collection.put(cleanedList.get(0).substring(cleanedList.get(0).lastIndexOf("/") + 1), saveSet);
		cleanedList.remove(0);
		Set<OWLAxiom> unionSet = new HashSet<OWLAxiom>(differenceSet);
		for (String path : fileList) {
			Set<OWLAxiom> returnSet = this.getAxioms(new File(path));
			unionSet.addAll(returnSet);
			// If returnSet is empty, an error occured. We skip this.
			if (!returnSet.isEmpty()) {
				differenceSet.retainAll(returnSet);
				collection.put(path.substring(path.lastIndexOf("/") + 1), returnSet);
			}
		}
		unionSet.removeAll(differenceSet);
		Set<OWLAxiom> selfjust = this.getAssertedAxioms(original, unionSet);
		System.out.println("Removing all self-justifications...");
		unionSet.removeAll(selfjust);
		System.out.println("Done! Saving to ontology file...");
		this.saveDifferences(unionSet, savePath);
		System.out.println("Done! Getting self-justification list...");
		addResult("disagreement_size", "" + unionSet.size());
		addResult("self_just_size", "" + selfjust.size());
		addResult("normaliser", "" + approach);
		return this.getSJList(collection, selfjust);
	}

	// Removes any funky ontologies (empties, broken)
	// Will produce a flag to be caught
	private ArrayList<String> cleanList(ArrayList<String> fileList) {
		ArrayList<String> cleanedList = new ArrayList<String>(fileList);
		for (String path : fileList) {
			File loadingFile = new File(path);
			if (!loadingFile.exists() || loadingFile.length() == 0) {
				cleanedList.remove(path);
				System.out.println("Error with: " + path);
			}
		}
		return cleanedList;
	}

	// Gets failures and success for selfjusts
	// and puts them in arraylist
	private ArrayList<String> getSJList(Map<String, Set<OWLAxiom>> collection, Set<OWLAxiom> disagreements) {
		ArrayList<String> details = new ArrayList<String>();
		for (OWLAxiom ax : disagreements) {
			String detail = ax.toString();
			String yay = ",";
			String nay = ",";
			for (String s : collection.keySet()) {
				if (collection.get(s).contains(ax)) {
					yay = yay + s + ";";
				} else {
					nay = nay + s + ";";
				}
			}
			detail = detail + " " + yay + nay;
			details.add(detail);
		}
		return details;
	}

	// Gets all axioms originally asserted in the ontology. We ignore
	// delcarations
	// as these aren't part of any classification and reasoners vary on whether
	// or not
	// to include them in their output class hiearchy.
	private Set<OWLAxiom> getAssertedAxioms(OWLOntology ontology, Set<OWLAxiom> disagreements) {
		Set<OWLAxiom> selfjust = new HashSet<OWLAxiom>();
		for (OWLAxiom ax : disagreements) {
			if (ontology.containsAxiom(ax) && !ax.isOfType(AxiomType.DECLARATION)) {
				selfjust.add(ax);
			}
		}
		return this.filter(selfjust);
	}

	// Loads ontology from file, gets axioms then removes ontology.
	// Performs normalisation.
	// Spits out empty set in case ontology loading fails, is
	// empty, inconsistent or is unavailable.
	private Set<OWLAxiom> getAxioms(File file) {
		Set<OWLAxiom> returnSet = new HashSet<OWLAxiom>();
		Map<String, String> rowcsv = new HashMap<String, String>();
		rowcsv.put("filename", ""+ file.getPath().substring(file.getPath().lastIndexOf("/") + 1));
		try {
			if (file.exists() && file.length() != 0) {
				OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
				loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
				loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
				OWLOntology onto = ontoman.loadOntologyFromOntologyDocument((new FileDocumentSource(file)),
						loaderConfig);
				OWLReasoner r = new Reasoner.ReasonerFactory().createReasoner(onto);
				boolean consistent = r.isConsistent();
				if (approach.equals("ore")) {
					if (consistent) {
						ClassHierarchyNormaliser chn = new ClassHierarchyNormaliser();
						returnSet.addAll(chn.loadClassificationResultDataIntoOntology(onto).getAxioms());
					}

				} else if (approach.equals("bails")) {
					if (consistent) {
						returnSet.addAll(OntologyClassification.getInferredHierarchy(ontoman, r, onto).getAxioms());

					} else {
						// returnSet.add(ontoman.getOWLDataFactory().getOWLSubClassOfAxiom(ontoman.getOWLDataFactory().getOWLThing(),
						// ontoman.getOWLDataFactory().getOWLNothing()));
					}

				} else {
					if (consistent) {
						returnSet.addAll(onto.getAxioms());
					}
				}
				rowcsv.put("consistent", consistent + "");
				rowcsv.put("inf_ch_size", "" + returnSet.size());
				ontoman.removeOntology(onto);
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		csv.add(rowcsv);
		return returnSet;
	}

	// Filtering of axioms - remove declarations and tautologies. At the moment
	// tautologies are
	// just defined w.r.t classification. Later versions will require something
	// more complex.
	private Set<OWLAxiom> filter(Set<OWLAxiom> cleanSet) {
		Set<OWLAxiom> filter = new HashSet<OWLAxiom>();
		for (OWLAxiom ax : cleanSet) {
			if (ax.isOfType(AxiomType.DECLARATION)) {
				filter.add(ax);
			} else {
				// Sanity check
				if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
					if (this.isTautology((OWLSubClassOfAxiom) ax)) {
						filter.add(ax);
					}
				}
			}
		}
		cleanSet.removeAll(filter);
		return cleanSet;
	}

	private void saveDifferences(Set<OWLAxiom> differences, String savePath) {
		Set<OWLAxiom> cleanedDifferences = this.filter(differences);
		try {
			OWLOntology diffOntology = ontoman.createOntology(cleanedDifferences);
			File saveFile = new File(savePath);
			try {
				ontoman.saveOntology(diffOntology, IRI.create(saveFile));
			} catch (OWLOntologyStorageException e) {
				System.out.println("Failed to save OWLOntology at " + savePath);
				e.printStackTrace();
			}
		} catch (OWLOntologyCreationException e) {
			System.out.println("Failed to create OWLOntology on difference set.");
			e.printStackTrace();
		}
	}

	private Boolean isTautology(OWLSubClassOfAxiom ax) {
		return ax.getSubClass().isOWLNothing() || ax.getSuperClass().isOWLThing();
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}
}
