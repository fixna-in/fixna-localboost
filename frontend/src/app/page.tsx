import { HealthProbe } from "./providers";

export default function HomePage() {
  return (
    <section aria-labelledby="home-heading">
      <h1 id="home-heading">Fixna LocalBoost</h1>
      <p>
        AI-assisted local advertising orchestration for SMBs — plan, launch and
        measure hyperlocal campaigns across Google, Meta and WhatsApp (mock
        adapters in demo mode).
      </p>
      <HealthProbe />
    </section>
  );
}

