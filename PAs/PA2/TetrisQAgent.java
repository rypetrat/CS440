package src.pas.tetris.agents;


// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;



// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.Module;
import edu.bu.tetris.utils.Coordinate;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;


public class TetrisQAgent
    extends QAgent
{

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction() {
        // can experiment switching between Tahn,ReLU,Sigmoid to see which performs the best or maybe even multiple, i think Tahn + ReLU is our best bet tho
        final int inputSize = 5; // should equal the size of the input row-vector
        final int hiddenDim = (int)Math.pow(inputSize, 2); // increasing this value will allow for more complex patterns to be learned but will also increase the risk of overfitting
        final int outDim = 1; // always keep at 1
        
        Sequential qFunction = new Sequential();

        // hidden layer 1
        qFunction.add(new Dense(inputSize, hiddenDim));
        qFunction.add(new ReLU()); 
 
        // out dimension
        qFunction.add(new Dense(hiddenDim, outDim));
        return qFunction;
    }

    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */
    @Override
    public Matrix getQFunctionInput(final GameView game, final Mino potentialAction) {
        Matrix gameMatrix = null; // init matrix var

        // init feature matrix that will be returned with all the features of importance
        Matrix featureMatrix = Matrix.zeros(1, 5);

        // init features data (can add or remove certain features to see how it performs w/wo them)
        int maxHeight = 22; // height of the tallest column accounting for the current mino placement, gives distance from the top
        boolean heightSet = false;
        
        int bumpiness = 0; // sum of the absolute differences in height between adjacent columns
        Integer[] colHeights = new Integer[10];

        int emptyBelow = 0; // counts number of empty spaces below the highest placed mino for each column

        int fullRows = 0; // counts the number of full rows that will get cleared
        
        int minoType = -1; // gets the type of the mino to be placed next in int form
        Mino.MinoType curMino = potentialAction.getType(); 

        try {
            // Get the grayscale image of the game board
            gameMatrix = game.getGrayscaleImage(potentialAction);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // parses through grayscale image matrix to collect feature data
        for (int y = 0; y < gameMatrix.getShape().getNumRows(); y++ ) {
            boolean isFull = true; // resets for each row
            for (int x = 0; x < gameMatrix.getShape().getNumCols(); x++) {
                // already placed piece coordinate
                if (gameMatrix.get(y, x) == 0.5 || gameMatrix.get(y, x) == 1.0) {
                    // sets maxHeight
                    if (heightSet == false) {
                        maxHeight = y;
                        heightSet = true;
                    }
                    // sets max height for this column for bumpiness
                    if (colHeights[x] == null) {
                        colHeights[x] = y;
                    }
                    // iterate fullRow when we get to the end of the row and isFull is still true
                    if (x == 9 && isFull == true) {
                        fullRows += 1;
                    }
                }
                // empty coordinate
                else if (gameMatrix.get(y, x) == 0.0) {
                    // iterates empty below when some cord above the current empty cord is filled
                    if(colHeights[x] != null) {
                        emptyBelow += 1;
                    }
                    // if we find any open hole in the row then this row is not full
                    isFull = false;
                }
            }
        }

        // set bumpiness value
        for (int i = 0; i < 9; i++) {
            int cur = 22;
            if (colHeights[i] != null) {
                cur = colHeights[i];
            }

            int next = 22;
            if (colHeights[i+1] != null) {
                next = colHeights[i+1];
            }
            bumpiness += Math.abs(cur - next);
        }

        // set value of minoType
        if (curMino == Mino.MinoType.valueOf("I")) {
            minoType = 0;
        }
        else if (curMino == Mino.MinoType.valueOf("J")) {
            minoType = 1;
        }
        else if (curMino == Mino.MinoType.valueOf("L")) {
            minoType = 2;
        }
        else if (curMino == Mino.MinoType.valueOf("O")) {
            minoType = 3;
        }
        else if (curMino == Mino.MinoType.valueOf("S")) {
            minoType = 4;
        }
        else if (curMino == Mino.MinoType.valueOf("T")) {
            minoType = 5;
        }
        else if (curMino == Mino.MinoType.valueOf("Z")) {
            minoType = 6;
        }
        
        // prints for each feature data point
        //System.out.println("maxHeight: " + maxHeight);
        //System.out.println("bumpiness: " + bumpiness);
        //System.out.println("emptyBelow: " + emptyBelow);
        //System.out.println("fullRows: " + fullRows);
        //System.out.println("minoType: " + minoType);
        

        // set values in the return matrix to the collected feature data values
        featureMatrix.set(0, 0, maxHeight);
        featureMatrix.set(0, 1, bumpiness);
        featureMatrix.set(0, 2, emptyBelow);
        featureMatrix.set(0, 3, fullRows);
        featureMatrix.set(0, 4, minoType);

        //System.out.println(featureMatrix);
        //System.out.println(gameMatrix);

        // return features data
        return featureMatrix;
    }

    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    @Override
    public boolean shouldExplore(final GameView game, final GameCounter gameCounter) {
        // gets turn and game ID's
        int turnIdx = (int)gameCounter.getCurrentMoveIdx();
        int gameIdx = (int)gameCounter.getCurrentGameIdx();

        // fine tune for testing
        double INITIAL_EXPLORATION_RATE = 0.99 - ((gameIdx * 0.0015));  // scale the gameIdx's coef to total number of training games
        double FINAL_EXPLORATION_RATE = 0.01; // explore rate will not go lower than this value
        int EXPLORATION_DECAY_STEPS = 10000; // higher number = slower decay

        // calculates explore rate value
        double explore = Math.max(FINAL_EXPLORATION_RATE, INITIAL_EXPLORATION_RATE - turnIdx * (INITIAL_EXPLORATION_RATE - FINAL_EXPLORATION_RATE) / EXPLORATION_DECAY_STEPS);
        //System.out.println("exploration value: " + explore);

        // generates random number between 0-1 and if less than our explore value we ignore the policy
        if (this.getRandom().nextDouble() <= explore) { 
            //System.out.println("Policy ignored.");
            return true;
        }
        return false;
    }

    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    @Override
    public Mino getExplorationMove(final GameView game) {
        // gets the total number of possible actions that can be made at the current state
        int permutes = game.getFinalMinoPositions().size();

        // init matrix that will hold each q-value
        Matrix results = Matrix.zeros(1, permutes);

        // parses through each possible action and passes that along with the gamestate to getQ to get its q-value from forward
        for (int i = 0; i < permutes; i++) {
            try {
                Matrix cur = this.getQFunctionInput(game, game.getFinalMinoPositions().get(i)); // get the row-vector for each permute to input into the NN
                results.set(0, i, Math.exp(this.initQFunction().forward(cur).get(0, 0))); // exponentiate the returned q-value and set in results
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        // sum all collected q-values from results matrix to qtotal
        double qTotal = results.sum().get(0, 0);

        // set summation value to a 1x1 matrix 
        Matrix denom = Matrix.zeros(1, 1);
        denom.set(0, 0, qTotal);

        // ediv each value by the summation value
        Matrix finalResults = null;
        try {
            finalResults = results.ediv(denom);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // find and return smallest value in finalResults
        int minInd = 0;
        double minVal = Double.POSITIVE_INFINITY;
        for (int j = 0; j < permutes; j++) {
            if (finalResults.get(0, j) < minVal) {
                minVal = finalResults.get(0, j);
                minInd = j;
            }
        }
        return game.getFinalMinoPositions().get(minInd);
    }

    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game) {
        Board b = game.getBoard(); // current gamestate board
        double reward = 0.0; // final reward value for the current state

        int maxHeight = 22; // y value of the highest occupied coordinate on the board
        Coordinate highestCord = null;
        Boolean highestFound = false;
        
        int emptyBelow = 0; // counts number of empty spaces below the highest placed mino for each column
        Integer[] colMax = new Integer[10];

        int bumpiness = 0; // sum of the absolute differences in height between adjacent columns

        int clearedRows = 0; // number of fully empty rows below the highest occupied position signifying the row was full and cleared
        

        // parse through board to collect feature data
        for (int y = 0; y < 22; y++) {
            boolean isClear = true;
            for (int x = 0; x < 10; x++) {
                // occupied coordinate
                if (b.isCoordinateOccupied(x, y)) { 
                    // get the highest coordinate position
                    if (highestFound == false) {
                        highestCord = new Coordinate(x, y);
                        highestFound = true;
                    }
                    // sets the max for the current column
                    if (colMax[x] == null) {
                        colMax[x] = y;
                    }
                    isClear = false;
                }

                // empty coordinate
                else if (!b.isCoordinateOccupied(x, y)) {
                    // check if current column has an occupied space in a higher row
                    if(colMax[x] != null) {
                        emptyBelow += 1;
                    }
                    if(x == 9 && isClear == true && highestFound == true) {
                        clearedRows += 1;
                    }
                }
            }
            if (highestCord != null) {
                highestFound = true;
            }
        }

        // set maxHeight
        if (highestCord != null) {
            maxHeight = highestCord.getYCoordinate();
        }

        // set bumpiness
        for (int i = 0; i < 9; i++) {
            int cur = 22;
            if (colMax[i] != null) {
                cur = colMax[i];
            }

            int next = 22;
            if (colMax[i+1] != null) {
                next = colMax[i+1];
            }
            bumpiness += Math.abs(cur - next);
        }

        // set ptsEarned
        int ptsEarned = game.getScoreThisTurn();


        // prints for each feature data point
        //System.out.println("ptsEarned: " + ptsEarned);
        //System.out.println("clearedRows: " + clearedRows);
        //System.out.println("maxHeight: " + maxHeight);
        //System.out.println("emptyBelow: " + emptyBelow);
        //System.out.println("bumpiness: " + bumpiness);

        // calculate reward value, highly weights getting any points 
        reward = ((50 * ptsEarned) + (10 * clearedRows)) - ((bumpiness + emptyBelow) / (double)maxHeight);

        //System.out.println("Reward value: " + reward);
        //System.out.println();

        return reward;
    }
}