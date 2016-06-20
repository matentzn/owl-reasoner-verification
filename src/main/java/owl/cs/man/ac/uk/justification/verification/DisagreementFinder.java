package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class DisagreementFinder {

	/**
	Takes Class Hierachies stored as .owl or .xml files
	finds all disagreements between them (the difference 
	between the union of all of them and the intersection
	of all of them) and then stores it as .owl file. 
	**/
	
	private OWLOntologyManager ontoman;
	
	public DisagreementFinder(){
		this.ontoman = OWLManager.createOWLOntologyManager();
	}
	
	public void getDifferences(ArrayList<String> fileList, String savePath){
		try {
			OWLOntology checkOntology = ontoman.loadOntologyFromOntologyDocument(new File(fileList.get(0)));
			fileList.remove(0);
			Set<OWLAxiom> differenceSet = checkOntology.getAxioms();
			ontoman.removeOntology(checkOntology);
			Set<OWLAxiom> unionSet = new HashSet<OWLAxiom>();
			for(String path:fileList)
			{
				OWLOntology checkOntology2 = ontoman.loadOntologyFromOntologyDocument(new File(path));
				//if appears totally fucked, ignore
				unionSet.addAll(checkOntology2.getAxioms());
				differenceSet.retainAll(checkOntology2.getAxioms());
				ontoman.removeOntology(checkOntology2);
			}
			unionSet.removeAll(differenceSet);
			int i = 0;
			for(OWLAxiom ax:unionSet)
			{
				if(!ax.isOfType(AxiomType.DECLARATION))
				{
					System.out.println(ax);
					i++;
				}
			}
			System.out.println(i);
			OWLOntology diffOntology = ontoman.createOntology(unionSet);
			File saveFile = new File(savePath);
			ontoman.saveOntology(diffOntology, IRI.create(saveFile));
			/**ontoman.removeOntology(checkOntology);
			for(String path:fileList)
			{
				OWLOntology checkOntology2 = ontoman.loadOntologyFromOntologyDocument(new File(path));
				//if appears totally fucked, ignore
				differenceSet.addAll(this.setDifference(checkSet, checkOntology2.getAxioms()));
				ontoman.removeOntology(checkOntology2);
			}
			int i = 0;
			for(OWLAxiom ax:differenceSet)
			{
				if(!ax.isOfType(AxiomType.DECLARATION))
				{
					System.out.println(ax);
					i++;
				}
			}
			System.out.println(i);
			OWLOntology diffOntology = ontoman.createOntology(differenceSet);
			File saveFile = new File(savePath);
			ontoman.saveOntology(diffOntology, IRI.create(saveFile));
			**/
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public Set<OWLAxiom> setDifference(Set<OWLAxiom> set1, Set<OWLAxiom> set2){
		//Set<OWLAxiom> diffSet = new HashSet<OWLAxiom>();
		Set<OWLAxiom> union = new HashSet<OWLAxiom>();
		Set<OWLAxiom> intersection = new HashSet<OWLAxiom>();
		union.addAll(set1);
		union.addAll(set2);
		set1.retainAll(set2);
		intersection.addAll(set1);
		union.removeAll(intersection);
		return union;
		/**
		Set<OWLAxiom> bigSet;
		Set<OWLAxiom> smallSet;
		if(set1.size() > set2.size())
		{
			bigSet = set1;
			smallSet = set2;
		}
		else
		{
			bigSet = set2;
			smallSet = set1;
		}
		for(OWLAxiom ax:bigSet)
		{
			if(!smallSet.contains(ax))
			{
				diffSet.add(ax);
			}
			else
			{
				smallSet.remove(ax);
			}
		}
		diffSet.addAll(smallSet);
		return diffSet;
		**/
	}
	
}
