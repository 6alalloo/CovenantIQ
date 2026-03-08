# CovenantIQ Beginner Guide

## Purpose

This guide is for a demo viewer or general end user who understands software, but has no banking or finance background.

It explains what the app is doing in plain English, what the main screens mean, and how to move through the product without needing to understand commercial lending jargon first.

## What This App Is, In Plain English

CovenantIQ helps a team watch over business loans after those loans have already been issued.

The app keeps track of:
- the loan itself
- the rules attached to that loan
- the financial data submitted by the borrower
- the system's evaluation of whether the borrower is staying within the agreed rules
- alerts, comments, documents, and workflow decisions related to that loan

You can think of it as a monitoring dashboard for loan health.

## The Core Idea Without Finance Jargon

If you ignore the banking language, the app works like this:

1. A company has a record in the system.
2. That record has a set of rules it is expected to stay within.
3. New data gets submitted over time.
4. The system checks the new data against those rules.
5. If something looks wrong or risky, the system raises an alert.
6. Users review the alert, discuss it, attach evidence, and move it through a workflow.

That is the main product loop.

## Plain-English Terms

These terms appear throughout the app. You do not need domain expertise to use them if you keep these definitions in mind.

### Loan
A business account being monitored.

### Borrower
The company that received the loan.

### Covenant
A rule or condition the borrower is expected to meet.

Plain-English example:
- "This company must keep certain financial numbers above or below an agreed threshold."

### Financial Statement
A periodic data submission that contains the company's financial numbers.

In app terms, this is the input the system uses to check loan health.

### Covenant Result
The system's verdict for one covenant after checking a statement.

Plain-English meaning:
- pass = the rule was met
- breach = the rule was not met

### Alert
A signal that something needs attention.

Not every alert means disaster. It means the system wants a human to look at something.

### Breach
A stronger signal that a rule was not met.

### Early Warning
A weaker signal that something may be moving in the wrong direction even if it has not fully failed yet.

### Risk Summary
A rolled-up view of how risky a loan currently looks based on the latest available data.

### Workflow
A controlled sequence of states used to move work from one status to another.

Plain-English meaning:
- the app does not just store alerts, it also helps manage what stage each alert or item is in

### Ruleset
A versioned set of decision rules the app can apply.

You can think of this as configurable logic behind some evaluations.

### Change Control
A place to manage and approve structured changes to important configuration such as rules or workflows.

## What Kind Of User This Guide Assumes

This guide assumes you are one of these:
- a person reviewing a demo
- a new user exploring the product
- a technically comfortable user who needs to understand the app behavior, not the banking theory behind it

It does not assume you are:
- a credit analyst
- a banker
- a risk officer
- an administrator

## Before You Start

You will usually need:
- access to a running CovenantIQ environment
- a user account provided by whoever is running the demo
- the correct role to see certain areas of the app

Important:
- some pages are role-restricted
- if a page is missing or blocked, that is often a permissions issue, not user error

Based on the repo, some areas are limited by role, especially portfolio, integrations, and admin user management.

## The Simplest Mental Model For The Product

If you only remember one thing, remember this:

`loan -> rules -> submitted data -> system evaluation -> alerts -> human review`

Most screens are a different view of one step in that chain.

## Recommended First Tour

If you are new, do not start with the most technical screens.

Use this order:
1. Dashboard
2. Loans
3. One loan's Overview
4. Statements
5. Results
6. Alerts
7. Comments
8. Activity
9. Reports
10. Settings

Leave these for later unless you specifically need them:
- Portfolio
- Workflows
- Policies
- Change Control
- Integrations
- Admin Users

## Screen-By-Screen Guide

### 1. Dashboard

Start here if you want a quick sense of what the app is tracking.

What to look for:
- overall activity
- high-level health signals
- places where the system is surfacing something important

If you know nothing about the domain, use the Dashboard as a summary screen, not as a place to make detailed decisions.

### 2. Loans

This is the main entry point for understanding the app.

What this screen is for:
- browsing existing monitored loans
- finding a borrower record
- opening the full detail view for one loan

How to use it as a beginner:
- pick any loan record
- ignore whether the financial values are good or bad for now
- focus on how information is organized around the selected loan

### 3. Loan Overview

This is the simplest "understand this record" page.

What you should expect here:
- who the borrower is
- basic loan details
- a compact summary of the current state

As a beginner, use this page to answer:
- What entity am I looking at?
- Is this record active?
- Does it currently look calm or risky?

### 4. Statements

This page holds the submitted financial data over time.

Plain-English meaning:
- this is the raw input history the app uses to evaluate the loan

As a beginner, you do not need to understand every financial field.
You only need to understand that newer statements give the app more recent information to evaluate.

Typical use:
- view prior submissions
- add a new statement if your role allows it
- use this page when you want to understand where later results came from

### 5. Results

This is where the app shows how it evaluated the submitted data against the configured rules.

Plain-English meaning:
- this is the scorecard for the rules attached to the loan

How to read it:
- each row is usually one rule check
- a passing result means the borrower stayed inside the rule
- a breach means the borrower failed that specific check

If you are new, this page matters more than the raw statement page because it converts raw numbers into a clear pass/fail style outcome.

### 6. Alerts

This page shows the items that need human attention.

How to read it:
- alerts are the action queue
- open or unresolved alerts matter more than old resolved ones
- some alerts indicate a direct rule failure, while others are warning signs

As a beginner, ask these questions:
- Is there an active issue?
- How severe does the app think it is?
- Has someone acknowledged or resolved it?

### 7. Collateral / Exceptions

This area is more specialized.

Plain-English meaning:
- collateral is pledged backing tied to the loan
- exceptions are approved cases where a normal rule may be temporarily bypassed or handled differently

If you are just learning the app, you can safely treat this as an advanced area unless the demo specifically focuses on exception handling.

### 8. Documents

This page is for attached files related to submitted statements or loan review.

Use it when you want supporting material, not when you want the system's decision.

### 9. Comments

This is the human discussion layer.

What it is for:
- adding context
- explaining unusual results
- recording follow-up notes

As a beginner, this is often the best place to understand why a result or alert exists.

### 10. Activity

This page shows the audit trail.

Plain-English meaning:
- who did what, and when

Use it when you want history rather than current status.

### 11. Reports

Use this when you want a more packaged output view rather than investigating one item at a time.

### 12. Settings

Use this for user-level or environment-level adjustments that are exposed in the UI.

### 13. Portfolio

This is a broader cross-loan view, not a single-loan view.

If you have access, it is useful for answering:
- Which loans need the most attention overall?
- How does the full monitored book look at a glance?

This area is more useful after you already understand one-loan workflows.

### 14. Workflows

This is an advanced configuration area.

Plain-English meaning:
- it defines how items move between states

If you are just trying to use the product, you usually do not need to change anything here.

### 15. Policies

This is another advanced area.

Plain-English meaning:
- it deals with versioned rules and how decision logic is managed

Think of this as system configuration, not day-to-day review.

### 16. Change Control

This is for managing important changes in a controlled, reviewable way.

Use it if your job is to approve configuration changes. Otherwise, treat it as an advanced governance screen.

### 17. Integrations

This area handles webhook subscriptions and delivery history.

Plain-English meaning:
- it lets the app notify other systems when important events happen

This is mainly for technical operators or admins.

### 18. Admin Users

This is for user and role management.

Most general users will not need this.

## A Safe Beginner Workflow

If you want to experience the app without needing finance knowledge, use this simple workflow:

1. Open the Dashboard.
2. Go to Loans.
3. Open one loan.
4. Read the Overview page.
5. Go to Results and identify whether anything is passing or breaching.
6. Go to Alerts and see whether the app raised any action items.
7. Open Comments to see the human explanation.
8. Open Activity to understand what happened over time.

That path gives you the core product story without needing to understand the underlying finance formulas.

## A Slightly Deeper Workflow

Once the simple path makes sense, use this one:

1. Open a loan.
2. Review its Statements page to see the input history.
3. Review Results to see how the app interpreted that input.
4. Review Alerts to see what requires attention.
5. Review Comments and Documents for context.
6. Review Activity for timeline and accountability.

This path helps you understand cause and effect.

## How To Interpret What You See

### If a loan has statements but no obvious issues
That usually means the system evaluated the submitted data and did not find major problems worth surfacing as active alerts.

### If a loan has breaches
That means at least one configured rule was not met for the evaluated statement.

### If a loan has early warnings
That means the system noticed a concerning pattern or direction, even if the strict rule may not have fully failed.

### If alerts exist but are resolved
That means an issue was raised and later closed by a user.

### If a page is empty
Possible reasons include:
- no data has been created yet
- your role cannot access the relevant records or actions
- the selected loan simply has no entries for that section yet

## What You Do Not Need To Know At First

To use the app at a basic level, you do not need to know:
- lending regulations
- accounting formulas
- how a covenant is negotiated in real life
- the full difference between every alert type
- how rulesets or workflows are configured behind the scenes

At the start, you only need to understand:
- data goes in
- the app evaluates it
- the app highlights issues
- people review those issues

## Common Beginner Mistakes

### Mistake: Trying to understand every number first
Better approach:
- start with Overview, Results, and Alerts
- use raw statements only when you need more detail

### Mistake: Assuming every alert means failure
Better approach:
- some alerts are warnings, not final failures
- read status, severity, and comments together

### Mistake: Thinking an empty page means the app is broken
Better approach:
- first check whether the selected loan has any data for that section
- then consider whether your role is limited

### Mistake: Starting in admin/configuration areas
Better approach:
- learn the single-loan workflow first
- leave policies, workflows, integrations, and change control for later

## If You Need To Demo The App To Someone Else

Use this script:

1. "This app monitors business loans over time."
2. "Each loan has rules attached to it."
3. "When new financial data is submitted, the app checks whether those rules still hold."
4. "If something looks wrong or risky, the app creates alerts for human review."
5. "Users can then review results, discuss the issue, attach evidence, and track actions."

That explanation is accurate enough for a non-domain audience without dragging them into finance terminology.

## Where To Go If You Get Stuck

If you are unsure what a page means, fall back to this sequence:
- Overview for context
- Results for system judgment
- Alerts for items needing attention
- Comments for human explanation
- Activity for history

If that still does not help, ask the demo owner:
- what role your account has
- whether the environment is using seeded demo data
- whether the current demo is focused on end-user review or admin configuration

## Related Docs

For run/setup details, see `docs/RUNBOOK.md`.
For business/domain intent, see `docs/BRD.md`.
