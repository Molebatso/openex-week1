"""
LangChain agent powered by a local Ollama model.

Uses a ReAct-style loop to select tools from `market_tools` and
compose a final answer. Runs completely offline — no cloud AI.
"""

from langchain_ollama import ChatOllama
from langchain.agents import AgentExecutor, create_react_agent
from langchain_core.prompts import PromptTemplate
from langchain_core.tools import BaseTool

import config

_SYSTEM_PROMPT = """\
You are an AI trading assistant for OpenEx, a simulated cryptocurrency exchange.
You have access to the following tools to look up the user's portfolio data:

{tools}

Rules you MUST follow:
1. You are READ-ONLY. Never attempt to place, modify, or cancel orders.
2. Always use a tool to fetch live data; do not make up numbers.
3. Be concise and clear. Format numbers with commas and 2 decimal places for USD.
4. If a tool fails, say so honestly.

Use this EXACT format:

Question: the user's question
Thought: decide which tool to use
Action: the tool name (one of [{tool_names}])
Action Input: the input for the tool
Observation: the tool result
... (repeat Thought/Action/Action Input/Observation as needed)
Thought: I now have enough information to answer
Final Answer: your answer to the user

Begin!

Question: {input}
Thought:{agent_scratchpad}"""

_PROMPT = PromptTemplate.from_template(_SYSTEM_PROMPT)


def build_agent(tools: list[BaseTool]) -> AgentExecutor:
    """
    Build an AgentExecutor backed by the configured Ollama model.
    Called once at service startup.
    """
    llm = ChatOllama(
        model=config.OLLAMA_MODEL,
        base_url=config.OLLAMA_BASE_URL,
        temperature=0.1,
    )

    agent = create_react_agent(llm=llm, tools=tools, prompt=_PROMPT)

    return AgentExecutor(
        agent=agent,
        tools=tools,
        verbose=True,
        handle_parsing_errors=True,
        max_iterations=6,
        return_intermediate_steps=False,
    )
