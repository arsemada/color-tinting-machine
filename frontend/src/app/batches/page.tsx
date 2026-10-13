import { BatchHistory } from "@/components/batches/BatchHistory";

export default function BatchesPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">History</h1>
      <BatchHistory />
    </div>
  );
}
