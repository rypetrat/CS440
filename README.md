# CS440 PA1

//////////////////////////////////////////////////////////////////////////////////////////////////

Running (inculde "-d [diffucluty]" for difficulty scaling) (-s for no render)

javac -cp "./lib/*;." @battleship.srcs

java -cp "./lib/*;." edu.bu.battleship.Main --p1Agent src.pas.battleship.agents.ProbabilisticAgent

//////////////////////////////////////////////////////////////////////////////////////////////////

Difficulty

Easy (10x10): 1 carriers, 1 battleships, 1 destroyers, 1 submarines, 1 patrols

Medium (20x20): 1 carriers, 1 battleships, 1 destroyers, 2 submarines, 3 patrols

Hard (30x30): 2 carriers, 3 battleships, 4 destroyers, 4 submarines, 5 patrols

//////////////////////////////////////////////////////////////////////////////////////////////////

Ship Sizes

1 carriers = 5, battleships = 4, destroyers = 3, submarines = 3, patrols = 2

//////////////////////////////////////////////////////////////////////////////////////////////////

# CS440 PA2

//////////////////////////////////////////////////////////////////////////////////////////////////

Running:

javac -cp "./lib/*;." @tetris.srcs

-q src.pas.tetris.agents.TetrisQAgent -p 5000 -t 100 -v 50 -n 0.01 -b 5000

//////////////////////////////////////////////////////////////////////////////////////////////////

Functions To Complete:

- getQFunctionInput(GameView, Mino): Convert both objects into row vector for input to the NN with some fixed size, should contain all necessary information for the NN to give a representative q-value rank {OUTPUT: vector of GameView and Mino of some fixed size}

- getReward(GameView): Reward function, calculate reward for being in that state of the game, bad state -> small/negative whereas good state-> large/positive {OUTPUT: reward value based on GameView}

- initQFunction(inputVector): Build actual NN, can only build feed-forward, should expect fixed size of input vector from getQ, should output an unbounded scalar q-value {OUTPUT: scalar q-value based on the inputVector}

- shouldExplore: Agent curiosity that encourages the agent to ignore the policy to search for unique experiences, return True when it should ignore the policy to find a unique experience and False otherwise {OUTPUT: True/False for if it will follow the policy}

- getExplorationMove: How we can generate an action that should lead to a new experience {OUTPUT: some sense of curiosity that can lead to previously undiscovered experience}
