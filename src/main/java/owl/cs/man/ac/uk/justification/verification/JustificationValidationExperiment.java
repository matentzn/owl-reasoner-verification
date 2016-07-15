package owl.cs.man.ac.uk.justification.verification;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.ore.wrappers.OREv2ReasonerWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;
import owl.cs.man.ac.uk.experiment.experiment.ReasonerExperiment;

public class JustificationValidationExperiment extends ReasonerExperiment {


	public JustificationValidationExperiment(File ontfile, File csvfile, File inferred_hierachy, String reasonername,
			int reasoner_timeout) {
		super(ontfile, csvfile, inferred_hierachy, reasonername, reasoner_timeout);		
	}

	@Override
	protected void process() throws Exception {
		System.out.println("Reasoner: " + getReasonerName());
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		long startload = System.nanoTime();
		OWLOntology o = manager.loadOntologyFromOntologyDocument(getOntologyFile());
		long endload = System.nanoTime();
		OWLAxiom axiom = getAxiom(o);
		if(!axiom.equals(null))
		{
			System.out.println("Checking entailment: "+ axiom);
			long start = System.currentTimeMillis();
			OWLReasoner reasoner = createReasoner(o);
			Boolean answer = reasoner.isEntailed(axiom);
			long end = System.currentTimeMillis();
			addResult("filename",getOntologyFile().getName());
			addResult("entailment",axiom.toString());
			addResult("reasoner_verdict",answer.toString());
			addResult("entailment_checking_time", "" + (end - start));	
	    }
		else
		{
			System.out.println("Error, null entailment.");
		}
		addResult("ontology_loading_time", "" + (endload - startload));
	}
	
	//Reused method from isomatch, will need a better method to deal
	//with more complex entailments.
	private OWLAxiom getAxiom(OWLOntology o) {
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		Set<OWLClass> anjuClass = o.getClassesInSignature();
		anjuClass.add(df.getOWLThing());
		anjuClass.add(df.getOWLNothing());
		OWLClass subclass = null;
		OWLClass supclass = null;
		Boolean cond1 = false;
		Boolean cond2 = false;
		for(OWLClass cl:anjuClass)
		{
			Set<OWLAnnotationAssertionAxiom> anno = cl.getAnnotationAssertionAxioms(o);
			if(!anno.isEmpty())
			{
				for(OWLAnnotationAssertionAxiom aax : anno)
				{
					if(aax.getAnnotation().getValue().toString().contains("superclass") && !cond1)
					{
							cond1 = true;
							supclass = cl;
					}
					else if(aax.getAnnotation().getValue().toString().contains("subclass") && !cond2)
					{
							cond2 = true;
							subclass = cl;
					}
				}
				
			}
		}
		if(cond1 && cond2)
		{
			return df.getOWLSubClassOfAxiom(subclass, supclass);
		}
		else 
		{
			return null;
		}
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}
	
	
}
