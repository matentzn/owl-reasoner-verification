package owl.cs.man.ac.uk.justification.verification;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.Version;

import owl.cs.man.ac.uk.experiment.classification.OntologyClassification;
import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;
import owl.cs.man.ac.uk.experiment.experiment.Experiment;
import owl.cs.man.ac.uk.experiment.metrics.StaticMetrics;
import owl.cs.man.ac.uk.experiment.ontology.OntologyUtilities;
import owl.cs.man.ac.uk.experiment.ontology.TautologyChecker;
import owl.cs.man.ac.uk.experiment.util.ReasonerUtilities;

public class JustificationTimeExperiment extends Experiment {

	private final String reasoner_string;
	private final Set<String> reasoner = new HashSet<String>();

	private final File inf1;
	private final File just_out;
	OWLReasonerConfiguration conf;
	private final int limit_just;
	private final int limit_analysis;
	private final boolean dont_compute_inf;

	public JustificationTimeExperiment(File ontologyFile, File csvFile,
			File just_out, String r1, File inf1, int reasoner_timeout,
			int limit_just, int limit_analysis, boolean dont_compute_inf) {
		super(ontologyFile, csvFile);
		this.reasoner_string = r1;
		this.just_out = just_out;
		this.inf1 = inf1;
		this.limit_just = limit_just;
		this.limit_analysis = limit_analysis;
		this.dont_compute_inf = dont_compute_inf;
		this.reasoner.addAll(Arrays.asList(this.reasoner_string.split(",")));
		long rto = reasoner_timeout / this.reasoner.size();
		conf = new SimpleConfiguration(rto);
		System.out.println("Set up experiment " + this.toString());
		System.out.println("Justification limit: " + limit_just);
		System.out.println("Reasoner timeout: " + rto);
		addResult("reasoner_timeout_indiv", "" + rto);
	}

	public void process() throws FileNotFoundException, RuntimeException,
			OWLOntologyCreationException, OWLOntologyStorageException {
		System.out.println("Process started..");

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology o = manager
				.loadOntologyFromOntologyDocument(getOntologyFile());

		Map<String, OWLOntology> inferredHierarchies = computeInferredHierarchies(o);

		writeInfHierResults(inferredHierarchies);

		System.out.println("Computing intersection..");

		Map<String, Map<String, Set<OWLAxiom>>> aggreement = new HashMap<String, Map<String, Set<OWLAxiom>>>();
		Set<OWLAxiom> fullintersection = new HashSet<OWLAxiom>();
		Set<OWLAxiom> fullunion = new HashSet<OWLAxiom>();

		computeAgreement(inferredHierarchies, aggreement, fullintersection,
				fullunion);

		addResult("full_union_size", fullunion.size() + "");
		addResult("full_intersection_size", fullintersection.size() + "");
		addResult("number_opinions", aggreement.keySet().size() + "");
		// printpause(aggreement.size()+"");

		boolean flipped_coin = false;
		boolean total_aggreement = (aggreement.keySet().size() == 1);
		boolean total_disagreement = (aggreement.keySet().size() == reasoner
				.size());
		int maj_ct = 1;

		addResult("flipped_coin", flipped_coin + "");
		addResult("total_aggreement", total_aggreement + "");
		addResult("total_disagreement", total_disagreement + "");

		Map<String, Set<String>> majority_tmp = new HashMap<String, Set<String>>();
		Set<String> majority = new HashSet<String>();

		if (!total_aggreement && !total_disagreement) {
			int size = 0;

			// Determine largest majority
			for (String uuid : aggreement.keySet()) {
				if (aggreement.get(uuid).keySet().size() > size) {
					size = aggreement.get(uuid).keySet().size();
				}
			}

			// get all majorities
			int i = 1;
			for (String uuid : aggreement.keySet()) {
				if (aggreement.get(uuid).keySet().size() == size) {
					majority_tmp.put(i + "", new HashSet<String>());
					majority_tmp.get(i + "").addAll(
							aggreement.get(uuid).keySet());
					i++;
				}
			}
			maj_ct = majority_tmp.keySet().size();
			int flip = randInt(1, maj_ct);
			// printpause(aggreement.get(uuid).keySet().size()+"");
			majority.addAll(majority_tmp.get(flip + ""));

			if (maj_ct > 1) {
				flipped_coin = true;
			}
			addResult("number_majority", maj_ct + "");
		}

		aggreement.clear();

		Set<OWLAxiom> irregular_entailments = new HashSet<OWLAxiom>(fullunion);

		irregular_entailments.removeAll(fullintersection);
		fullunion.clear();
		fullintersection.clear();

		Set<OWLSubClassOfAxiom> irregular_entailments_filtered = new HashSet<OWLSubClassOfAxiom>();
		extractSubClassOfAxioms(irregular_entailments,
				irregular_entailments_filtered);

		System.out.println("Done. Irregular Entailment size: "
				+ irregular_entailments_filtered.size());

		addResult("disagreed_entailment_size",
				irregular_entailments_filtered.size() + "");

		System.out.println("Computing justifications..");

		Set<String> bug_suspect = new HashSet<String>();
		Set<String> unclear_reasoner = new HashSet<String>();
		Set<String> unclear_justification = new HashSet<String>();

		for (String r_just : reasoner) {
			System.out.println("Justification reasoner: " + r_just);
			OWLReasonerFactory df_just = ReasonerUtilities.getFactory(r_just);
			ExplanationGeneratorFactory<OWLAxiom> genFac = ExplanationManager
					.createExplanationGeneratorFactory(df_just);
			// Now create the actual explanation generator for our ontology
			ExplanationGenerator<OWLAxiom> gen = genFac
					.createExplanationGenerator(o);

			for (OWLSubClassOfAxiom entailment : irregular_entailments_filtered) {
				String entailmentstr = entailment.getSubClass().asOWLClass()
						.getIRI().getFragment()
						+ "_"
						+ entailment.getSuperClass().asOWLClass().getIRI()
								.getFragment();

				System.out.println("Computing explanations for entailment: "
						+ entailmentstr);
				Set<Explanation<OWLAxiom>> expl = new HashSet<Explanation<OWLAxiom>>();
				
				try {
					if(limit_just>0) {
						expl.addAll(gen.getExplanations(entailment, limit_just));
					}
					else {
						expl.addAll(gen.getExplanations(entailment));	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				List<Explanation<OWLAxiom>> expl_analysis = getSmallestExplanations(
						expl, limit_analysis);

				System.out.println("Analysing explanations: "
						+ expl_analysis.size());
				
				for (Explanation<OWLAxiom> explanation : expl_analysis) {
					File justfile = new File(new File(new File(just_out,
							getOntologyFile().getName()), entailmentstr),
							r_just);
					justfile.mkdirs();
					String justname = "just_" + explanation.getSize() + "_"
							+ r_just + "_" + entailmentstr + "_"
							+ System.currentTimeMillis() + ".owl";
					for (String r : reasoner) {
						System.out
								.println("Check entailmented by justification with reasoner: "
										+ r);
						OWLReasonerFactory df = ReasonerUtilities.getFactory(r);
						Map<String, String> data = new HashMap<String, String>();
						data.put("reasoner", r);
						data.put("reasoner_just", r_just);
						data.put("ontology", getOntologyFile().getName());
						data.put("just_size", explanation.getSize() + "");
						data.put("entailment", entailmentstr);
						data.put("justname", justname);
						data.put("subclass", entailment.getSubClass()
								.asOWLClass().getIRI().toURI().toString());
						data.put("superclass", entailment.getSuperClass()
								.asOWLClass().getIRI().toURI().toString());
						OntologySerialiser
								.saveOWLXML(justfile,
										manager.createOntology(explanation
												.getAxioms()), justname,
										manager);
						OWLOntology just = manager.createOntology(explanation
								.getAxioms());
						boolean entailed = false;
						try {
							OWLReasoner r_c = df.createReasoner(just);
							entailed = r_c.isEntailed(entailment);
						} catch (Exception e) {
							e.printStackTrace();
							data.put("just_verification_exception", e
									.getClass().getSimpleName());
						}
						boolean inf_computed = inferredHierarchies
								.containsKey(r);
						boolean in_o = inf_computed ? inferredHierarchies
								.get(r).containsAxiom(entailment, true) : false;

						String category = "inconspicuous";
						if (!in_o) {
							if (entailed) {
								bug_suspect.add(r);
								category="bug";
							} else {
								category="tie";
								unclear_reasoner.add(r);
								unclear_justification.add(r_just);
							}
						}
						else {
							if (!entailed) {
								category="odd";
							}
						}
						data.put("entailed_by_j", entailed + "");
						data.put("in_cl_o", in_o + "");
						data.put("maj_ct", maj_ct + "");
						data.put("inf_computed", inf_computed + "");
						data.put("category", category);
						CSVUtilities.appendCSVData(new File(just_out,
								"metadata.csv"), data);
					}
				}
			}
		}
		StaticMetrics sm = new StaticMetrics(o, manager);
		addResult(sm.getEssentialMetrics());
		List<Map<String, String>> reasoner_verdicts = new ArrayList<Map<String, String>>();

		for (String r : reasoner) {
			Map<String, String> data = new HashMap<String, String>();
			data.put("majority", majority.contains(r) + "");
			data.put("reasoner", r);
			data.put("unclear_reasoner", unclear_reasoner.contains(r) + "");
			data.put("unclear_justification", unclear_justification.contains(r)
					+ "");
			data.put("bug_suspect", bug_suspect.contains(r) + "");
			data.put("ontology", getOntologyFile().getName());
			data.put("coin_flipped", flipped_coin + "");
			data.put("total_agreement", total_aggreement + "");
			data.put("total_disagreement", total_disagreement + "");
			data.put("computed_inf", inferredHierarchies
					.containsKey(r) + "");
			
			boolean mv_diff_nobug = (!majority.contains(r) && !bug_suspect.contains(r) && !unclear_reasoner.contains(r) && !unclear_justification.contains(r));
			data.put("mv_diff_nobug", mv_diff_nobug + "");
			
			boolean mv_diff_bug = (majority.contains(r) && bug_suspect.contains(r));
			data.put("mv_diff_bug", mv_diff_bug + "");
			
			reasoner_verdicts.add(data);
		}

		CSVUtilities.writeCSVData(new File(just_out, "reasoner_verdicts.csv"),
				reasoner_verdicts, true);
	}

	public void writeInfHierResults(Map<String, OWLOntology> inferredHierarchies) {
		addResult("inf_hier_size", "" + inferredHierarchies.size());

		for (String r : reasoner) {
			boolean infhier = false;
			if (inferredHierarchies.containsKey(r)) {
				infhier = true;
			}
			addResult("inf_hier_" + r, "" + infhier);
		}
	}

	public Map<String, OWLOntology> computeInferredHierarchies(OWLOntology o)
			throws OWLOntologyCreationException {
		Map<String, OWLOntology> inferredHierarchies = new HashMap<String, OWLOntology>();
		System.out.println("Loading inferred hierarchies..");
		for (String r : reasoner) {
			System.out.println("Reasoner: " + r);
			OWLOntologyManager inf_man = OWLManager.createOWLOntologyManager();
			OWLReasonerFactory rf = ReasonerUtilities.getFactory(r);
			String filename = "inf_" + r + "_" + getOntologyFile().getName();
			File info = new File(inf1, filename).isFile() ? new File(inf1,
					filename) : new File(inf1, filename.replaceAll(
					"_indexed.owl.xml$", ""));

			if (info.isFile()) {
				System.out.println("File exists..");
				OWLOntology o_inf = inf_man
						.loadOntologyFromOntologyDocument(info);
				inferredHierarchies.put(r, o_inf);
			} else if(dont_compute_inf) {
				System.out.println("Not computing in hier for "+r);
			}
			else {
				System.out
						.println("File does not exist, computing inferred hierarchy..");
				try {
					OWLReasoner reasoner = rf.createReasoner(o, conf);
					OWLOntology o_inf = OntologyClassification
							.getInferredHierarchy(inf_man, reasoner, o);
					inferredHierarchies.put(r, o_inf);
					OntologyUtilities.saveOntologyMergedOWLXML(info, o_inf,
							info.getName());
				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		}
		return inferredHierarchies;
	}

	public boolean computeAgreement(
			Map<String, OWLOntology> inferredHierarchies,
			Map<String, Map<String, Set<OWLAxiom>>> aggreement,
			Set<OWLAxiom> fullintersection, Set<OWLAxiom> fullunion) {
		boolean first = true;
		for (String r : inferredHierarchies.keySet()) {
			System.out.println(r);
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			for (OWLAxiom ax : inferredHierarchies.get(r).getAxioms()) {
				OWLAxiom ax_wo_anno = ax.getAxiomWithoutAnnotations();
				if (ax.isLogicalAxiom()) {
					axioms.add(ax_wo_anno);
				}
			}
			fullunion.addAll(axioms);
			if (first) {
				fullintersection.addAll(axioms);
				first = false;
			} else {
				fullintersection.retainAll(axioms);
			}

			boolean equal = false;
			for (String uuid : aggreement.keySet()) {
				// printpause("key: "+key);
				Map<String, Set<OWLAxiom>> group = aggreement.get(uuid);
				for (String rcomp : group.keySet()) {
					// System.out.println("CMP:" + rcomp);
					Set<OWLAxiom> union = new HashSet<OWLAxiom>(axioms);
					Set<OWLAxiom> comprax = group.get(rcomp);
					// System.out.println(comprax.size());
					// System.out.println(axioms.size());
					union.addAll(comprax);
					Set<OWLAxiom> intersection = new HashSet<OWLAxiom>(axioms);
					intersection.retainAll(comprax);

					union.removeAll(intersection);
					// for (OWLAxiom ax : union)
					// System.out.println(ax);
					// printpause(r+" "+rcomp+" DIFF: "+union.size());
					// printpause(r+" "+rcomp+" DIFF: "+union.size());
					if (union.isEmpty()) {
						equal = true;
						aggreement.get(uuid).put(r, axioms);

					}
					break;
				}
				if (equal) {
					break;
				}
			}
			if (!equal) {
				String rnd = UUID.randomUUID().toString();
				aggreement.put(rnd, new HashMap<String, Set<OWLAxiom>>());
				aggreement.get(rnd).put(r, axioms);
			}
		}
		return first;
	}

	public void printpause(String s) {
		System.out.println(s);
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runJustificationChecker(OWLOntologyManager manager,
			OWLOntology o, OWLReasonerFactory f1, OWLReasonerFactory f2,
			OWLOntology o_inf1, OWLOntology o_inf2, String r1, String r2)
			throws OWLOntologyCreationException, FileNotFoundException,
			OWLOntologyStorageException {

	}

	private List<Explanation<OWLAxiom>> getSmallestExplanations(
			Set<Explanation<OWLAxiom>> expl, int limit) {
		List<Explanation<OWLAxiom>> expl_small = new ArrayList<Explanation<OWLAxiom>>();
		Map<Integer, Set<Explanation<OWLAxiom>>> ex_map = new HashMap<Integer, Set<Explanation<OWLAxiom>>>();
		for (Explanation<OWLAxiom> e : expl) {
			Integer size = e.getSize();
			if (!ex_map.containsKey(size)) {
				ex_map.put(size, new HashSet<Explanation<OWLAxiom>>());
			}
			ex_map.get(size).add(e);
		}
		List<Integer> sizes = new ArrayList<Integer>(ex_map.keySet());
		Collections.sort(sizes);
		for (Integer size : sizes) {
			expl_small.addAll(ex_map.get(size));
		}
		if (limit < expl_small.size()) {
			return expl_small.subList(0, limit);
		} else {
			return expl_small;
		}

	}

	public void extractSubClassOfAxioms(Set<OWLAxiom> irregular_entailments,
			Set<OWLSubClassOfAxiom> o1_ax) {
		for (OWLAxiom ax : irregular_entailments) {
			if (ax instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom) ax;
				OWLClassExpression sub = sax.getSubClass();
				OWLClassExpression superc = sax.getSuperClass();
				if (sub.isClassExpressionLiteral()
						&& superc.isClassExpressionLiteral()) {
					if (!TautologyChecker.isTautology(sax)) {
						o1_ax.add(sax.getAxiomWithoutAnnotations());
					}
				}
			} else if (ax instanceof OWLEquivalentClassesAxiom) {
				OWLEquivalentClassesAxiom eax = (OWLEquivalentClassesAxiom) ax;
				for (OWLSubClassOfAxiom sax : eax.asOWLSubClassOfAxioms()) {
					OWLClassExpression sub = sax.getSubClass();
					OWLClassExpression superc = sax.getSuperClass();
					if (sub.isClassExpressionLiteral()
							&& superc.isClassExpressionLiteral()) {
						if (!TautologyChecker.isTautology(sax)) {
							o1_ax.add(sax.getAxiomWithoutAnnotations());
						}
					}
				}
			}

		}
	}

	@Override
	public String getExperimentName() {
		return "SimpleClassificationTime";
	}

	@Override
	protected Version getExperimentVersion() {
		return new Version(1, 0, 0, 1);
	}

	public int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
}
