import { Suspense } from "react";
import { NewCampaignForm } from "./form";
import { LoadingState } from "../../providers";

export default function NewCampaignPage() {
  return (
    <Suspense fallback={<LoadingState label="Loading campaign form" />}>
      <NewCampaignForm />
    </Suspense>
  );
}

