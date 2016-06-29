package owl.cs.man.ac.uk.justification.verification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.experiment.Experiment;

public class DisgreementFinderExperiment extends Experiment{
	
	private ArrayList<String> classhierachyList = new ArrayList<String>();
	private OWLOntology ontology;
	private OWLOntologyManager ontoman;
	private File disagreementoutdir;
	private File sjcsv;
	
	public DisgreementFinderExperiment(
			File ontfile,
			File csvfile,
			File classhierarchydirf,
			File sjcsv,
			File disagreementoutdir) {
		super(ontfile,csvfile);
		this.ontoman = OWLManager.createOWLOntologyManager();
		this.disagreementoutdir = disagreementoutdir;
		this.sjcsv = sjcsv;
		try {
			this.ontology = ontoman.loadOntologyFromOntologyDocument(getOntologyFile());
			String oname = getOntologyFile().getName();
			for(File diso:classhierarchydirf.listFiles()) {
				if(diso.getName().contains(oname)) {
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
		ArrayList<String> selfjustList = this.getDifferences(classhierachyList, disagreementoutdir.getAbsolutePath() + "/" + getOntologyFile().getName() + "_disagreements.owl", ontology);
		System.out.println("Creating CSV of self-just disagreements.");
		PrintWriter outStr = new PrintWriter(new BufferedWriter(new FileWriter(sjcsv, true)));
		outStr.println("axiom,agree,disagree");
		for(String s:selfjustList)
		{
			outStr.println(s);
		}
		outStr.close();
	}
	
	public ArrayList<String> getDifferences(ArrayList<String> fileList, String savePath, OWLOntology original){
		ArrayList<String> cleanedList = this.cleanList(fileList);
		Set<OWLAxiom> differenceSet = this.getAxioms(new File(cleanedList.get(0)));
		Set<OWLAxiom> saveSet = new HashSet<OWLAxiom>(differenceSet);
		Map<String,Set<OWLAxiom>> collection = new HashMap<String,Set<OWLAxiom>>();
		System.out.println("Constructing disagreement set...");
		collection.put(cleanedList.get(0).substring(cleanedList.get(0).lastIndexOf("/") + 1), saveSet);
		cleanedList.remove(0);
		Set<OWLAxiom> unionSet = new HashSet<OWLAxiom>(differenceSet);
		for(String path:fileList)
		{
			Set<OWLAxiom> returnSet = this.getAxioms(new File(path));
			unionSet.addAll(returnSet);
			//If returnSet is empty, an error occured. We skip this.
			if(!returnSet.isEmpty())
			{
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
		System.out.println("Done! Getting self-justification list..." );
		return this.getSJList(collection, selfjust);
	}

	
	//Removes any funky ontologies (empties, broken)
	//Will produce a flag to be caught
	private ArrayList<String> cleanList(ArrayList<String> fileList){
		ArrayList<String> cleanedList = new ArrayList<String>(fileList);
		for(String path:fileList)
		{
			File loadingFile = new File(path);
			if(!loadingFile.exists() || loadingFile.length() == 0)
			{
				cleanedList.remove(path);
				System.out.println("Error with: " + path);
			}
		}
		return cleanedList;
	}
	
	//Gets failures and success for selfjusts 
	//and puts them in arraylist
	private ArrayList<String> getSJList(Map<String,Set<OWLAxiom>> collection,Set<OWLAxiom> disagreements){
		ArrayList<String> details = new ArrayList<String>();
		for(OWLAxiom ax:disagreements)
		{
			String detail = ax.toString();
			String yay = ",";
			String nay = ",";
			for(String s:collection.keySet())
			{
				if(collection.get(s).contains(ax))
				{
					yay = yay + s + ";";
				}
				else
				{
					nay = nay + s + ";";
				}
			}
			detail = detail + " " + yay + nay;
			details.add(detail);
		}
		return details;
	}
	
	//Gets all axioms originally asserted in the ontology. We ignore delcarations
	//as these aren't part of any classification and reasoners vary on whether or not
	//to include them in their output class hiearchy.
	private Set<OWLAxiom> getAssertedAxioms(OWLOntology ontology, Set<OWLAxiom> disagreements){
		Set<OWLAxiom> selfjust = new HashSet<OWLAxiom>();
		for(OWLAxiom ax:disagreements)
		{
			if(ontology.containsAxiom(ax) && !ax.isOfType(AxiomType.DECLARATION))
			{
				selfjust.add(ax);
			}
		}
		return selfjust;
	}
	
	//Loads ontology from file, gets axioms then removes ontology.
	//Spits out empty set in case ontology loading fails, is
	//empty or is unavailable.
	private Set<OWLAxiom> getAxioms(File file){
		Set<OWLAxiom> returnSet = new HashSet<OWLAxiom>();
		try {
			if(file.exists() && file.length() != 0){
				OWLOntology onto = ontoman.loadOntologyFromOntologyDocument(file);
				returnSet.addAll(onto.getAxioms());
				ontoman.removeOntology(onto);
			}
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnSet;
	}
	
	private void saveDifferences(Set<OWLAxiom> differences, String savePath){
		//Filtering of axioms - remove declarations and tautologies. At the moment tautologies are
		//just defined w.r.t classification. Later versions will require something more complex.
		Set<OWLAxiom> filter = new HashSet<OWLAxiom>();
		for(OWLAxiom ax:differences)
		{
			if(ax.isOfType(AxiomType.DECLARATION))
			{
				filter.add(ax);
			}
			else
			{	
				//Sanity check
				if(ax.isOfType(AxiomType.SUBCLASS_OF))
				{
					if(this.isTautology((OWLSubClassOfAxiom) ax))
					{
						filter.add(ax);
					}
				}
			}
		}
		differences.removeAll(filter);
		try {
			OWLOntology diffOntology = ontoman.createOntology(differences);
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
	
	private Boolean isTautology(OWLSubClassOfAxiom ax){
		return ax.getSubClass().isOWLNothing() || ax.getSuperClass().isOWLThing();
	}
	
	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}
}
