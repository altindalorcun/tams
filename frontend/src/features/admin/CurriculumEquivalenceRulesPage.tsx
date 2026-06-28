import { CurriculumEquivalenceRulesTab } from "./CurriculumEquivalenceRulesTab";

/**
 * Standalone admin page for curriculum equivalence rule management, linked from the sidebar.
 */
export function CurriculumEquivalenceRulesPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="sr-only">Müfredat Değişikliği Kuralları</h1>
      <CurriculumEquivalenceRulesTab />
    </div>
  );
}
