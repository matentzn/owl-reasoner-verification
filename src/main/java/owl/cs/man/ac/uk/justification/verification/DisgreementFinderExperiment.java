package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
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
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.ontology.TautologyChecker;

public class DisgreementFinderExperiment extends Experiment {

	private ArrayList<String> classhierachyList = new ArrayList<String>();
	private OWLOntology ontology;
	private OWLOntologyManager ontoman;
	private File disagreementoutdir;
	private File sjoutdir;
	private String approach;
	private long reasoner_time;
	private List<Map<String, String>> csv = new ArrayList<Map<String, String>>();
	public static IRI ASSERTION_ANNOTATION_PROPERTY_IRI = IRI.create("http://owl.cs.manchester.ac.uk/reasoner_verification/vocabulary#asserted_in");

	public DisgreementFinderExperiment(File ontfile, File csvfile, File classhierarchydirf, File sjoutdir,
			File disagreementoutdir,  String approach, long reasoner_time) {
		super(ontfile, csvfile);
		this.ontoman = OWLManager.createOWLOntologyManager();
		this.disagreementoutdir = disagreementoutdir;
		this.sjoutdir = sjoutdir;
		this.approach = approach;
		this.reasoner_time = reasoner_time;
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
		Map<OWLAxiom,ArrayList<String>> selfjustList = this.getDifferences(classhierachyList,
				disagreementoutdir.getAbsolutePath() + "/" + getOntologyFile().getName() + "_disagreements.owl",
				ontology);
		System.out.println("Creating ontology of self-justifications.");
		//PrintWriter outStr = new PrintWriter(new BufferedWriter(new FileWriter(sjcsv, true)));
		//outStr.println("axiom,agree,disagree");
		//for (String s : selfjustList) {
		//	outStr.println(s);
		//}
		//outStr.close();
		Map<OWLAxiom,Set<OWLAnnotation>> sjanno = this.annotateAxioms(selfjustList);
		this.saveDifferences(sjanno.keySet(), sjanno, new File(sjoutdir, getOntologyFile().getName() + "_sjdisagreements.owl").getAbsolutePath());
		long end = System.nanoTime();
		addResult("disagreement_finder_time", "" + (end - start));
		CSVUtilities.writeCSVData(new File(getCSVFile().getParent(), getOntologyFile().getName() + "_disagreement.csv"),
				csv, false);
	}

	public Map<OWLAxiom,ArrayList<String>> getDifferences(ArrayList<String> fileList, String savePath, OWLOntology original) {
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
		try {
			Set<OWLAxiom> delSet = TautologyChecker.getTautologies(unionSet);
			unionSet.removeAll(delSet);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Set<OWLAxiom> selfjust = this.getAssertedAxioms(original, unionSet);
		System.out.println("Removing all self-justifications...");
		unionSet.removeAll(selfjust);
		System.out.println("Done! Now annotating axioms according to presence...");
		Map<OWLAxiom,Set<OWLAnnotation>> outMap = this.annotateAxioms(this.getAssertedList(collection, unionSet));
		System.out.println("Done! Saving to ontology file...");
		this.saveDifferences(unionSet, outMap, savePath);
		System.out.println("Done! Getting self-justification list...");
		addResult("disagreement_size", "" + unionSet.size());
		addResult("self_just_size", "" + selfjust.size());
		addResult("normaliser", "" + approach);
		return this.getAssertedList(collection, selfjust);
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

	// Gets successes for entailments
	// and puts them in hashmap
	private Map<OWLAxiom,ArrayList<String>> getAssertedList(Map<String, Set<OWLAxiom>> collection, Set<OWLAxiom> disagreements) {
		Map<OWLAxiom,ArrayList<String>> details = new HashMap<OWLAxiom,ArrayList<String>>();
		for (OWLAxiom ax : disagreements) {
			ArrayList<String> yay = new ArrayList<String>();
			for (String s : collection.keySet()) {
				if (collection.get(s).contains(ax)) {
					yay.add(s);
				}
			}
			details.put(ax, yay);			
		}
		return details;
	}

	//takes axioms and annotates them to include info on assertions
	private Map<OWLAxiom,Set<OWLAnnotation>> annotateAxioms(Map<OWLAxiom,ArrayList<String>> assertionList){
		OWLDataFactory df = ontoman.getOWLDataFactory();
		OWLAnnotationProperty ap = df.getOWLAnnotationProperty(ASSERTION_ANNOTATION_PROPERTY_IRI);
		Map<OWLAxiom,Set<OWLAnnotation>> annoMap = new HashMap<OWLAxiom,Set<OWLAnnotation>>();
		for(OWLAxiom ax:assertionList.keySet())
		{
			Set<OWLAnnotation> returnSet = new HashSet<OWLAnnotation>();
			for(String s:assertionList.get(ax))
			{
				returnSet.add(df.getOWLAnnotation(ap, df.getOWLLiteral(s)));
			}
			annoMap.put(ax, returnSet);
		}
		return annoMap;
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
		return selfjust;
	}

	// Loads ontology from file, gets axioms then removes ontology.
	// Performs normalisation.
	// Spits out empty set in case ontology loading fails, is
	// empty, inconsistent or is unavailable.
	private Set<OWLAxiom> getAxioms(File file) {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		Set<OWLAxiom> returnSet = new HashSet<OWLAxiom>();
		Map<String, String> rowcsv = new HashMap<String, String>();
		rowcsv.put("filename", ""+ file.getPath().substring(file.getPath().lastIndexOf("/") + 1));
		try {
			if (file.exists() && file.length() != 0) {
				OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
				loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
				loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
				OWLOntology onto = manager.loadOntologyFromOntologyDocument((new FileDocumentSource(file)),
						loaderConfig);
				OWLReasoner r = new Reasoner.ReasonerFactory().createReasoner(onto,new SimpleConfiguration(reasoner_time));
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
				//rowcsv.put("consistent", consistent + "");
				//rowcsv.put("inf_ch_size", "" + returnSet.size());
				ontoman.removeOntology(onto);
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		csv.add(rowcsv);
		return returnSet;
	}

	private void saveDifferences(Set<OWLAxiom> differences, Map<OWLAxiom,Set<OWLAnnotation>> map, String savePath) {
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology diffOntology = manager.createOntology(differences);
			for(OWLAxiom ax:differences)
			{
				manager.addAxiom(diffOntology, ax.getAnnotatedAxiom(map.get(ax)));
			}
			File saveFile = new File(savePath);
			try {
				manager.saveOntology(diffOntology, IRI.create(saveFile));
			} catch (OWLOntologyStorageException e) {
				System.out.println("Failed to save OWLOntology at " + savePath);
				e.printStackTrace();
			}
		} catch (OWLOntologyCreationException e) {
			System.out.println("Failed to create OWLOntology on difference set.");
			e.printStackTrace();
		}
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}
}
