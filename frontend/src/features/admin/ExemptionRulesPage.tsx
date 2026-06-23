import { ExemptionRulesTab } from "./ExemptionRulesTab";

/**
 * Standalone admin page for exemption rule management, linked from the sidebar.
 */
export function ExemptionRulesPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="sr-only">Muafiyet Kuralları</h1>
      <ExemptionRulesTab />
    </div>
  );
}
