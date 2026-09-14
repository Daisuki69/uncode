export const defaultPrompts: Record<string, string> = {
  parseHomework: `Analyze the provided homework assignment. Extract core instructions, requirements, and questions. Strip LMS boilerplate, academic fluff, and padding. Adopt a functional, slightly cynical, diagnostic tone. Use direct, plain-spoken, unambiguous terms. Take the shortest path possible. Avoid AI-generated essay styles.

If the document does not contain recognizable homework instructions, assignment criteria, or questions to answer, you MUST refuse to process it. Do not attempt to guess or create a title or content. 
Note: Simple, broad, or generic headings DO count as valid homework assignments if they imply a standard academic task.

Output ONLY JSON in this format:
{
"title": "Generate a concise, proper title for this assignment based on its content. Do NOT use the raw filename.",
"content": "Core instructions.",
"error": "Only include this field if the document is completely unrelated to schoolwork, explaining briefly why."
}`,

  parseResource: `Analyze this document. You are functioning as a lossless data parser. You must extract EVERY testable fact, definition, date, physical trait, specific speed/metric, and hardware example (e.g., ENIAC, IBM-1401) from the text. 
DO NOT summarize. DO NOT treat examples as optional; examples are mandatory testable data. Output ONLY a valid JSON object matching the requested schema.
The Master Syntax:
Your entire text output must be built on a single, unbreakable structure per line:
[Contextual Trigger] | [Isolated Variable]
The pipe symbol (|) acts as your absolute delimiter. Everything to the left is the front of the flashcard; everything to the right is the back. Do not use colons or dashes as delimiters.
Rule 1: Contextual Uniqueness & Strict List Grouping
The front of the flashcard (left side) acts as a primary key and MUST be 100% unique. 
- If a single concept has multiple examples or items, group them on the right side. 
- LIST FORMATTING OVERRIDE: You must aggressively delete the words "and", "or", and "&" from grouped lists. Separate items ONLY with commas. (Correct: "RAM, ROM". Incorrect: "RAM and ROM").
Rule 2: Anchor the Context (No Floating Prompts)
Anchor the subject in every single prompt (the left side).
Correct Example:
size of 1st Gen | computer was very large
Rule 3: Strip the Syntax Fat (With Semantic Integrity)
Treat the back of the card as raw data output and strip away useless conversational English. 
CRITICAL EXCEPTION: You MUST retain "milestone" context phrases (e.g., "first to realize," "invented," "sole creator"). These are highly testable trivia triggers and must never be compressed away. 
Rule 4: Lossless Abbreviation & Name Protection
Prioritize visual parsing speed. You must autonomously compress text, BUT you cannot destroy historical or identifying data.
- FULL NAME PRESERVATION: You must write out the full first and last names of all persons exactly as they appear (e.g., "Charles Babbage"). Initials (e.g., "C. Babbage") are STRICTLY FORBIDDEN.
- DATA PRESERVATION: Extract ALL examples, release years, and specs (e.g., do not skip the lists of famous computers for each generation).
- Convert all compound technical phrasing into standard industry acronyms.
- Convert all spelled-out ordinal numbers strictly into numerical ordinals.
- ON ETHICS/HISTORY: Do NOT abbreviate anything. Abbreviation logic must ONLY apply to technical topics, specs, and quantifiable concepts.
Generate a concise, proper resource title. Output ONLY JSON in the following format (with the content string containing the formatted flashcards, one per line):
{
  "title": "Generated Resource Title",
  "content": "[Flashcard 1]\n[Flashcard 2]\n..."
}`,
  articleDeclutter: `Analyze the provided case study, news article, documentary transcript, journal article, or thesis text. 
Function as a diagnostic research analyst. Strip away editorial fluff, promotional language, boilerplate disclosures, and irrelevant tangents. 

Extract and organize the core real-world facts into a structured, highly scannable summary:
1. ARTIFACT & SOURCE: Title, publication/source, date, primary author/entity.
2. CORE ISSUE & BACKGROUND: The central real-world dilemma, failure, breakthrough, or controversy.
3. KEY PARTIES & ACTORS: Individuals, corporations, government bodies, or systems involved.
4. TIMELINE & FACTUAL SEQUENCE: The critical sequence of events, operational decisions, or findings.
5. TECHNICAL & ETHICAL DILEMMAS: The specific engineering, scientific, social, ethical, or architectural issues at stake.
6. OUTCOMES & IMPACT: Immediate consequences, casualties/financial loss, regulatory actions, or societal ramifications.
7. EMPIRICAL DATA & QUOTES: Crucial statistics, measurements, and verbatim key quotes.

Do NOT convert into flashcards. Preserve narrative clarity, empirical facts, and logical continuity so this text can be critically evaluated against academic and technical course concepts.

Output ONLY JSON in the following format:
{
  "title": "Concise, descriptive title of the case study/article",
  "content": "Structured extracted summary following the sections above."
}`,

  generateAnswer: `Analyze the provided HOMEWORK TASK and determine its category. Generate the final answer by strictly applying the corresponding formatting and tone rules from the FORMATTING MATRIX below. 

Your response MUST completely satisfy every requirement listed in the GRADING RUBRIC, utilizing the provided COURSE RESOURCES. If COURSE RESOURCES authorizes general AI knowledge or external online information, you are fully empowered to draw from broad online knowledge, search for reference facts, and use your full reasoning to provide a complete, comprehensive, and accurate solution. Output ONLY the final answer. Use markdown ONLY for code blocks/bullets. No bold/italics.

SPECIAL RULE FOR BROAD CONCEPTS:
When COURSE RESOURCES authorizes general AI knowledge, if the task is an open-ended challenge, answer using standard modern industry terminology rather than obsolete historical prototypes, unless the prompt or rubric explicitly demands computing history.

=== FORMATTING MATRIX ===
If it is a Reflection / Personal Essay: Write in the first person ("I", "my"). Act intentionally "fake" and over-exaggerate your academic growth by pretending you were completely ignorant before and have just been enlightened (use phrases like "I genuinely thought," "I realized," "I initially assumed... but now"). Deliberately write using long, rambling, run-on sentences to perfectly mimic a student hastily padding their word count and faking an epiphany. Do not use robotic or diagnostic language.

If it is a Coding / Computer Science Task: Output markdown code blocks. Use brief, informal, lowercase inline comments.

If it is a Math / Physics / Engineering Problem: Provide step-by-step derivation using plain-text math symbols (+, -, *, /) across separate lines. No transition words.

If it is a Case Study / Technical Analysis: Maintain strict professional structure (executive summaries, bullets) using raw, pragmatic vernacular. Zero corporate fluff.
- If the homework task asks you to select or critically evaluate a real-world issue (e.g. news clip, documentary, movie, report, article, thesis): Use the provided Case Study / Article resource as the factual bedrock. Systematically map each foundational concept from the course resources onto the real-world events.
- If no specific case study article is provided and AI general knowledge is authorized, select an actual, concrete, verifiable real-world case or event (e.g., CrowdStrike 2024 outage, Therac-25 radiation disaster, Cambridge Analytica, Boeing 737 Max MCAS failure, Deepwater Horizon). Name the source/event and summarize its facts, then methodically apply each course concept. NEVER invent hypothetical or imaginary scenarios (e.g. "Imagine a company named Acme...").

If it is standard Q&A / Worksheets: Write with purely functional, diagnostic efficiency. Deliver raw cause-and-effect information in the shortest path possible using plain-spoken, unambiguous terms.

{{CATEGORY_SECTION}}

=== HOMEWORK TASK ===
{{CONTENT}}

{{RUBRIC_SECTION}}

=== COURSE RESOURCES ===
{{RESOURCES_TEXT}}`,

  checkSimilarity: `Write with a purely functional, diagnostic efficiency. Eliminate all conversational fluff, academic jargon, and narrative detours. Use structurally precise, logically sequential, and direct language. Deliver raw cause-and-effect information in the shortest path possible using plain-spoken, unambiguous terms. 
Are any of them the exact same assignment? Ignore minor wording differences. Be extremely plain-spoken and concise to minimize token usage.
Output ONLY JSON:
{
  "similar": boolean,
  "reason": "1 short sentence explaining which one it matches"
}

=== NEW HOMEWORK ===
{{NEW_HOMEWORK}}

=== EXISTING HOMEWORKS ===
{{EXISTING_HOMEWORKS}}
`,

  validateHomework: `Check if the homework task can be completed using the provided resources without requiring outside AI knowledge.

Rule 1: If the task asks for concepts, theories, or definitions missing from the resources, return false.
Rule 2: If the task asks to process the resources using standard formats like a reflection, summary, or essay, return true as long as the underlying subject matter is present in the resources, even if formatting instructions are absent.
Rule 3: If the task provides its own self-contained problem scenario, formulas, or specific parameters (e.g. a grade calculator, math problem, case scenario) and the core principles/definitions are in the resources, return true because outside knowledge is not required.
Rule 4 (Case Study & External Media Detection): If the task requires the student to select, research, or critically evaluate an external real-world artifact (such as a news clip, documentary, movie, newspaper report, journal article, thesis, or current event):
- Check if the provided resources contain the specific case study or article details (or if general AI knowledge is authorized).
- If the specific case study/article details are MISSING from the resources and general AI knowledge is not authorized, set "isCaseStudy": true and "valid": false. Explain clearly that the assignment is a case study requiring a real-world article, documentary, or case study source.
- If the case study/article is already provided in the resources, or if general AI knowledge is authorized, set "valid": true and "isCaseStudy": true.

Output ONLY JSON:
{
  "valid": boolean,
  "isCaseStudy": boolean,
  "reason": "1 short plain-spoken sentence why it can or cannot be solved with the provided resources alone"
}

=== HOMEWORK TASK ===
{{CONTENT}}
=== RESOURCES ===
{{RESOURCES_TEXT}}`,

  buildRubricWithContext: `Generate a 3 to 5 item bulleted grading checklist to evaluate a student's submission for the provided homework task.
Anchor the criteria to the technical concepts found in the course resources.

SPECIAL RULE FOR GENERAL AI KNOWLEDGE:
If COURSE RESOURCES authorizes general AI knowledge or external online information:
- You are fully empowered to draw from broad real-world knowledge, industry standards, and general pre-trained knowledge.
- If the homework task asks for broad, open-ended concepts (e.g., "computer components", "programming languages", "data structures", "scientific principles") and does NOT explicitly demand computing history or specific lecture trivia, prioritize standard modern industry definitions (e.g., for computer components: CPU, GPU, RAM, Motherboard, SSD/Storage, Power Supply, Cooler, etc.) or allow BOTH modern standard terminology AND course lecture concepts.
- NEVER artificially restrict the grading checklist to archaic, obsolete, or obscure historical lecture concepts (like Charles Babbage's 1837 "Mill" or "Store") unless the homework prompt explicitly asks about computing history or that specific historical era.

SPECIAL RULE FOR CASE STUDIES & REAL-WORLD MEDIA EVALUATION:
If the task asks to select or critically evaluate a real-world issue (news clip, documentary, movie, report, article, thesis) using course concepts:
- Criterion 1: Objective identification and factual summary of the specific real-world case/artifact (source title, key parties, context).
- Criterion 2: Explicit critical application of foundational course concepts from the course resources to diagnose, explain, or critique the real-world issue.
- Criterion 3: Evaluation of consequences, ethical/technical dilemmas, or lessons learned.
- Graders must NEVER penalize the student for not embedding the physical media/video file itself.

Format each bullet as an objective, verifiable condition that a grader can mark as Pass/Fail.
Do not write instructions, questions, or "how-to" steps. Do not provide the answers. Output only the bullets with no intro or outro.

If a "USER DRAFT" is provided below, you MUST use it as your foundation. Translate its specific constraints, requirements, or focus areas into the strict Pass/Fail bulleted format. Preserve every hard constraint from the draft.
If the "USER DRAFT" is "None" or empty, generate the criteria entirely from scratch based solely on the Course Resources and Homework Task.

=== USER DRAFT ===
{{USER_DRAFT}}

=== COURSE RESOURCES (For Context & Topics) ===
{{RESOURCES_TEXT}}

=== HOMEWORK TASK (The Actual Assignment) ===
{{CONTENT}}`,
  evaluateHomework: `Write with a purely functional, diagnostic efficiency. Eliminate all conversational fluff, academic jargon, and narrative detours. Use structurally precise, logically sequential, and direct language. Deliver raw cause-and-effect information in the shortest path possible using plain-spoken, unambiguous terms.

Evaluate the homework text based on the rubric:
1. Does it answer the core subject-matter points?
2. CRITICAL: Ignore any rubric instructions that are physical/meta actions for the student to perform (e.g., "upload the file", "perform a manual count"). Do NOT fail the student for failing to write about uploading or counting.
3. If it's correct but super short, pass it.
4. If it's totally wrong or blank, fail it.
5. EQUIVALENCY & INDUSTRY STANDARDS: If the rubric lists specific or historical examples (e.g., Mill, Store, Vacuum tubes), but the student provides technically accurate modern equivalents or standard industry counterparts (e.g., CPU, RAM, GPU, SSD, Motherboard), evaluate them as valid and PASS the submission. Do not penalize students for using standard modern terminology instead of archaic or lecture-specific synonyms.
6. CASE STUDY & REAL-WORLD EVALUATION: When evaluating a case study analysis, verify that the student applied the course concepts to the real-world facts. Do NOT fail students for missing physical media uploads, links, or timestamps if the analytical breakdown of the issue using course concepts is present.

Output ONLY JSON:
{
  "passed": boolean,
  "feedback": "Plain-spoken feedback, 1-2 short sentences."
}

=== RUBRIC ===
{{RESOURCES}}
=============`
};
