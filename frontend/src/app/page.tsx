import { HealthProbe } from "./providers";
import Link from "next/link";

export default function HomePage() {
  return <section className="landing" aria-labelledby="home-heading">
    <div className="landing-hero"><span className="story-pill">BIG IDEAS. LOCAL IMPACT.</span><h1 id="home-heading">Local business.<br /><em>Remarkable potential.</em></h1><p>Your neighbourhood is full of opportunity. Bring your audience, campaigns, and insights together with Fixna LocalBoost.</p><div className="hero-actions"><Link className="button" href="/register">Build your workspace ↗</Link><Link className="button button-secondary" href="/dashboard">Explore workspace</Link></div><p className="hero-note">AI-assisted planning. Human-approved execution.</p></div>
    <div className="feature-grid">
      <article className="feature-card"><span className="feature-number">01 / PLAN</span><h2>Start with your neighbourhood.</h2><p>Define your business, locations, audience, and budget around the people you want to reach.</p></article>
      <article className="feature-card"><span className="feature-number">02 / CREATE</span><h2>A little intelligence. A lot of possibility.</h2><p>Use advisory AI recommendations to shape your strategy and creatives. You review and approve every step.</p></article>
      <article className="feature-card"><span className="feature-number">03 / MEASURE</span><h2>See what happens next.</h2><p>Keep campaign metrics and lead progress in one workspace, from first enquiry to conversion.</p></article>
    </div>
    <div className="landing-bottom"><p>Platform execution currently uses mock adapters for Google, Meta, and WhatsApp.</p><HealthProbe /></div>
  </section>;
}

