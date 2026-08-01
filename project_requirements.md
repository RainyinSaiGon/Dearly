# Final Project: Secure Virtual Assistant with Speaker Recognition

## 1. Overview

In recent years, virtual assistants have become increasingly popular and have been adopted across a wide range of domains. These systems can support users in performing various tasks, such as:

- Retrieving information
- Setting reminders
- Controlling devices
- Accessing personal data
- Modifying system settings

However, not every function of a virtual assistant should be executed immediately without additional verification. For sensitive or important tasks, the system must authenticate the user before allowing the requested action to be performed. In addition, speaker identification can be used to personalize the user experience for appropriate functions.

In this project, students will develop a virtual assistant that integrates:

- **Speaker Verification (SV)** for important tasks.
- **Speaker Identification (SID)** for personalization.

Specifically, students will:

- Select a speaker verification or speaker identification model.
- Train and evaluate the model using a suitable dataset.
- Integrate the trained model into a complete virtual assistant application.

---

# 2. Requirements

## Requirement 1 (5 points)

Students must select a representative model for **speaker verification** or **speaker identification**, such as:

- ECAPA-TDNN
- RawNet3
- Or an equivalent model

A suitable dataset must be used to train and evaluate the model.

The report must clearly describe:

- Dataset
- Training, validation, and test splits
- Selected model
- Training procedure
- Evaluation metrics
- Experimental results

---

## Requirement 2 (5 points)

Using the model developed in **Requirement 1**, students must build a complete virtual assistant that integrates **speaker verification** and **speaker identification**.

The system must be designed around specific use cases proposed by the students and satisfy the following requirements.

### Voice Interaction

The virtual assistant must support **voice-based interaction** with users.

The system must also provide a **speaker enrollment and management component**, such as:

- A web interface
- A desktop application
- Another simple application

This component is responsible for:

- Collecting initial voice data
- Managing information required for speaker verification and identification

---

### Required Assistant Functions

The system must include:

- At least **one general function** that **does not require authentication**
- At least **one important function** that can only be executed after **successful speaker verification**
- At least **one personalized function** that uses **speaker identification** to customize:
  - Responses
  - Information
  - Settings
  - Other user-specific behaviors

---

### Enrollment Procedure

The report must clearly describe the **speaker enrollment process**, including:

- How a new user's voice is collected
- How enrollment data is stored
- How information required for speaker verification and identification is maintained

---

### System Design

The report must clearly present:

- Overall system architecture
- Processing flow

---

# 3. Suggested Virtual Assistant Implementation

A typical virtual assistant may be organized as the following pipeline:

```
Speech Input
      │
      ▼
Automatic Speech Recognition (ASR)
      │
      ▼
Request Analysis & Task Orchestration
      │
      ├──────────────► Speaker Verification (for protected tasks)
      │
      ├──────────────► Speaker Identification (for personalization)
      │
      ▼
Execute Task
      │
      ▼
Text-to-Speech (TTS)
      │
      ▼
Voice Response
```

For important tasks, the system **must include Speaker Verification (SV)** before executing the requested action.

A **Speaker Identification (SID)** component may also be incorporated to personalize appropriate functions.

---

## Request Analysis & Task Orchestration

Students may implement this module using one of the following approaches.

### 1. Rule-based

Develop a collection of predefined rules and command patterns that directly map user requests to corresponding tasks.

---

### 2. Intent–Entity-based

Formulate the problem as:

- Intent classification
- Entity extraction

Then map the identified intent and entities to the corresponding action.

Frameworks such as **Rasa** may be used for this implementation.

---

### 3. LLM-based

Use **Large Language Models (LLMs)** to:

- Infer user intent
- Orchestrate the appropriate tasks
- Generate suitable responses

---

# 4. Submission Guidelines

Students must submit:

- Complete source code
- Trained model weights
- Training dataset
- Project report

All materials must be packaged into a **single ZIP file**.

Filename format:

```text
StudentID1_StudentID2_..._StudentIDN.zip
```

If the dataset or trained model is too large to include in the ZIP file:

- Upload the files to Google Drive.
- Submit a text file containing the corresponding link.

Filename format:

```text
StudentID1_StudentID2_..._StudentIDN.txt
```