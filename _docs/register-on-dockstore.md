---
title: Register with Dockstore
permalink: /docs/publisher-tutorials/register-on-dockstore/
---
<div class="alert alert-info">
This tutorial is a continuation of <a href="/docs/prereqs/getting-started-with-cwl/">Getting Started With CWL</a>, <a href="/docs/prereqs/getting-started-with-wdl/">Getting Started With WDL</a>, and <a href="/docs/prereqs/getting-started-with-nextflow/">Getting Started With Nextflow</a>. Please complete one or more of those tutorials that is relevant to you prior to doing this one.
</div>

# Registering with Dockstore
## Tutorial Goals
* Create a Dockstore account
* Link to relevant third party services

## Register on Dockstore
Before you can publish your new tool, you need to create a Dockstore account and link the relevant services. Dockstore supports login through GitHub using OAuth2. You can register at the [login/register page](https://www.dockstore.org/login).

You can also login with your Google Account. This is required to use the workflow sharing feature for users coming from Firecloud.

## Dockstore usernames

Your username will be visible in any public tool or workflow that you have. 

If you register with GitHub, we will default to your GitHub username. As of 1.5.0, if you register with Google, we will default to the email associated with your Google account.

### Changing your username

You can change your username in the onboarding wizard during setup or from the accounts page at the Dockstore Account Controls tab.

Currently you can only change your username when the following conditions are true:
* You do not have any published tools or workflows
* You do not have anything shared by you to other users through the permissions tab for workflows

For Google users, your initial username will include an @ symbol. We recommend you change your username to something that is not an email to avoid unwanted email.

## Linking With External Services

Once you register you can start linking your external accounts. There is also a command line tool we will use for most of the tasks in this tutorial. For this tutorial you only need to have your GitHub and Quay.io accounts established. However, Dockstore supports the following external services:
* GitHub
* Bitbucket
* GitLab
* Quay.io

Your link to GitHub is established on login and you will then be prompted to link your other accounts.

If you registered with Google, you will also need to link your GitHub account to follow along with the tutorial.

![Link accounts](/assets/images/docs/linking1.png)

<!-- Currently UI2 does not perform and automatic refresh all tools -->
<!-- Linking a supported image repository service (e.g. Quay.io) will automatically trigger a synchronization order to retrieve information about the account's tools

![Refresh tools](/assets/images/docs/linking2.png) -->

Below, GitHub, Google, and Quay.io accounts have been linked, it is necessary for at least the GitHub account or the Google account be linked in order to perform regular account activities.

![Link accounts completed](/assets/images/docs/linking3.png)

Next, the wizard will instruct you to setup the `dockstore` command line tool after linking your accounts, and upon completion you will be ready to use Dockstore.

## Next Steps

Follow the [next tutorial](/docs/publisher-tutorials/getting-started-with-dockstore/) to register your tool on Dockstore.