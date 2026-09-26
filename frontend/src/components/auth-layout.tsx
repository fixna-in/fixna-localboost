import type { ReactNode } from "react";
import { BrandMarkIcon } from "@/brand/brand-mark-icon";

export function AuthLayout({ children }: { children: ReactNode }) {
  return <div className="auth-layout">
    <aside className="auth-story" aria-labelledby="auth-story-title">
      <span className="story-pill">LOCAL AMBITION. SMARTER MARKETING.</span>
      <h2 id="auth-story-title">Your next customer<br />is closer than<br /><em>you think.</em></h2>
      <p>Turn your neighbourhood into your next growth opportunity. Plan, create, and measure your local campaigns in one place.</p>
      <div className="orbit-art" aria-hidden="true">
        <div className="orbit-ring ring-one" />
        <div className="orbit-ring ring-two" />
        <BrandMarkIcon className="orbit-core" width={67} height={70} />
        <span className="orbit-label orbit-label-one">◎ Find your audience</span>
        <span className="orbit-label orbit-label-two">↗ Grow your business</span>
        <span className="orbit-dot" />
      </div>
      <div className="story-footnote"><span aria-hidden="true">✦</span><div><strong>Intelligence that supports your decisions.</strong><small>AI-powered recommendations. Human-approved campaigns.</small></div></div>
    </aside>
    <div className="auth-panel">{children}<p className="auth-security">Your workspace. Your campaigns. Your control.</p></div>
  </div>;
}
