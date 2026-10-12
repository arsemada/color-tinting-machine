import { BatchHistory } from "@/components/batches/BatchHistory";

export default function BatchesPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Batch history</h1>
        <p className="text-sm text-muted">Completed quantities appear only after the machine reports them.</p>
      </div>
      <BatchHistory />
    </div>
  );
}
