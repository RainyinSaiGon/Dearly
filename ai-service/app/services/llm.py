"""LLM Service — OpenAI GPT-4o function-calling for intent routing."""

import os

# TODO (W5): Implement using openai SDK
# from openai import OpenAI

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")


class LLMService:
    def __init__(self):
        # self.client = OpenAI(api_key=OPENAI_API_KEY)
        pass

    async def classify_intent(self, transcript: str) -> dict:
        """
        Send transcript to GPT-4o with function definitions.
        Returns: {"intent": str, "entities": dict, "requires_sv": bool, "response": str}
        """
        raise NotImplementedError("LLM intent classification not yet implemented")
