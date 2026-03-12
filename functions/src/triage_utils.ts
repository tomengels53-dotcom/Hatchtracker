

export type PredictionClass = "bug" | "feature_request" | "usage_confusion" | "known_issue" | "spam" | "general";

export interface HatchyResponse {
    classification: PredictionClass;
    confidenceScore: number;
    suggestedResponse: string;
    requiresHumanReview: boolean;
}

/**
 * Triage logic for "Hatchy" Virtual Assistant.
 * Currently uses heuristic pattern matching.
 * TODO: Upgrade to Vertex AI (Gemini API) invocation.
 */
export async function triageTicket(
    description: string,
    category: { moduleName?: string; featureName?: string }
): Promise<HatchyResponse> {
    const text = description.toLowerCase();
    const module = (category.moduleName || "").toLowerCase();

    // Default: Needs Review
    let result: HatchyResponse = {
        classification: "general",
        confidenceScore: 0.5,
        suggestedResponse: `
Hi! Hatchy here 🐣.

I've received your ticket and have prioritized it for our team.

**Note on Wait Times:**
Our human admins are currently handling a high volume of requests or it may be outside of standard business hours. 
Please sit tight! I promise your ticket is in the queue and an expert will personally review it as soon as they are online. 🛡️

Thanks for your patience!
        `.trim(),
        requiresHumanReview: true // Always review default
    };

    // 1. Crash / Bug Detection
    if (text.includes("crash") || text.includes("error") || text.includes("exception") || text.includes("stops working")) {
        result = {
            classification: "bug",
            confidenceScore: 0.85,
            suggestedResponse: `
### Hatchy's Note 🐣
Oh no! It sounds like you've encountered a bug. 🛠️

I've flagged this for our engineering team to investigate immediately.

**Availability Update:**
Even if no system admin is actively online right now, this bug report triggers a special alert. 🚨 
Rest assured, it will get the necessary attention as soon as possible.

**Next Steps:**
- A developer will review your device logs.
- We might reach out if we need more details.

Thanks for helping us make HatchTracker better!
            `.trim(),
            requiresHumanReview: true // Bugs always need human eyes eventually
        };
    }
    // 2. Feature Requests
    else if (text.includes("wish") || text.includes("add") || text.includes("would be cool") || text.includes("support for")) {
        result = {
            classification: "feature_request",
            confidenceScore: 0.9,
            suggestedResponse: `
### Hatchy's Note 🐣
That's a great idea! 💡

I've added this to our "Feature Wishlist" for the product team. We love hearing how to make the app better for your flock.

While we can't promise a specific date, keep an eye on the "What's New" section in future updates!
            `.trim(),
            requiresHumanReview: false // Low risk
        };
    }
    // 3. Usage Confusion (Incubation)
    else if (module.includes("incubation") && (text.includes("how to") || text.includes("can't find") || text.includes("limit"))) {
        result = {
            classification: "usage_confusion",
            confidenceScore: 0.8,
            suggestedResponse: `
### Hatchy's Note 🐣
Hi there! It sounds like you might be having trouble with the Incubation setup.

**Quick Tip:**
- Ensure you have a **Device** registered with enough capacity.
- If your incubator is full, you might need to "Finish" or "Archive" an old hatch first.

Let me know if that clears things up!
            `.trim(),
            requiresHumanReview: false
        };
    }

    // 4. Billing Confusion
    else if (text.includes("subscription") || text.includes("cancel") || text.includes("refund") || text.includes("charged")) {
        result = {
            classification: "usage_confusion",
            confidenceScore: 0.95,
            suggestedResponse: `
### Hatchy's Note 🐣
I see this is regarding your subscription.

All billing is handled securely by Google Play.
- To **cancel** or **manage** your subscription, please open the Play Store -> Tap Profile -> Payments & Subscriptions.

If you believe there was an error with Premium features not unlocking, please let us know!
            `.trim(),
            requiresHumanReview: true // Financials always sensitive
        };
    }

    return result;
}
