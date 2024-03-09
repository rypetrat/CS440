package src.pas.battleship.agents;

// SYSTEM IMPORTS
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.List;

// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.ships.Ship.ShipType;
import edu.bu.battleship.game.EnemyBoard;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;


public class ProbabilisticAgent extends Agent {

    public ProbabilisticAgent(String name) {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
        
    }

    @Override
    public Coordinate makeMove(final GameView game)
    {
        // randomly picking positions to fire at before using improved logic, shit might have to stick to this lmao
        Random random = new Random();
        
        // gets the number of hits, misses, and untargeted coordinates from the current board, utimately this is gonna be useless unless i for some reason need these values
        int[] prevShots = this.shotTrack(game);
        int numHits = prevShots[0], numMiss = prevShots[1], numSunk = prevShots[2], numUnkw = prevShots[3];
        System.out.println("hits: " + numHits + " misses: " + numMiss + " sunk: " + numSunk + " unknown: " + numUnkw);
        
        // generate probability matrix based on the current board to make an informed next shot
        double[][] probMatrix = this.probabilitiesMatrix(game, numHits, numUnkw);

        // visual representation that prints out the probability matrix, also returns an arraylist of the adjacent coords and prints that out
        ArrayList<Integer[]> hitAdj = this.visRep(game, probMatrix); 
        System.out.print("Coordinates Adj to hits: ");   
        for (Integer[] x : hitAdj) {
            System.out.print("(");
            int z = 0;
            for (int y : x) {
                if (z % 2 == 0) {
                    System.out.print(y + ", ");
                }
                else {
                    System.out.print(y);
                }
                z +=1 ;
            }
            System.out.print(") ");
        }
        System.out.println();

        // call the nextshot function to generate the coordinate that should be fired at next
        Coordinate fire2 = this.nextShot(game, probMatrix, hitAdj);
        System.out.println("Highest probability shot: " + fire2.toString());

        // for separation spacing
        System.out.println();

        // if its not at the default OOB coord that i set then it gets fuckin nuked hell yea
        if (!fire2.equals(new Coordinate(100, 100))) {
            return fire2;
        }



        // all of the following should be removed as it is for the implemented random agent that will be phased out


        // picks random value that is in bounds
        int col = random.nextInt(game.getGameConstants().getNumCols());
        int row = random.nextInt(game.getGameConstants().getNumRows());

        //generates coordinate to fire at
        Coordinate fire = new Coordinate (row, col);

        // ensures no duplicate positions are fired upon for random shot picking
        while(!game.getEnemyBoardView()[fire.getXCoordinate()][fire.getYCoordinate()].toString().equals("UNKNOWN")) {
            col = random.nextInt(game.getGameConstants().getNumCols());
            row = random.nextInt(game.getGameConstants().getNumRows());
            fire = new Coordinate (row, col);
        }
        
        // shot to be fired
        return fire;
    }





    public double[][] probabilitiesMatrix(final GameView game, int hits, int unknown) {
        // calculates the number of remaining coordinates that can result in a hit, ultimately this will also be phased out by improved logic but its here now
        int remainingShipCoords = 1 + -hits;
        for(ShipType key : game.getEnemyShipTypeToNumRemaining().keySet()) {
            if (key.toString().equals("AIRCRAFT_CARRIER")) {
                // afc is 5 coordinates long
                remainingShipCoords += 5;
            }
            else if (key.toString().equals("BATTLESHIP")) {
                // bship is 4 coordinates long
                remainingShipCoords += 4;
            }
            else if (key.toString().equals("DESTROYER")) {
                // destroy is 3 coordinates long
                remainingShipCoords += 3;
            }
            else if (key.toString().equals("SUBMARINE") || key.toString().equals("PATROL_BOAT")) {
                // sub and pboat are 2 coordinates long
                remainingShipCoords += 2;
            }
        }
        // generate the matrix that will hold all the probability values for each coordinate
        double[][] probMat = new double[game.getGameConstants().getNumRows()][game.getGameConstants().getNumCols()];

        // nested for loop that will go and set the values for each coordinate in the matrix
        for(int y = 0; y < game.getGameConstants().getNumCols(); y++) {
            for(int x = 0; x < game.getGameConstants().getNumRows(); x++) {
                if (game.getEnemyBoardView()[x][y].toString().equals("HIT")) {
                    // set the value of a hit coordinate to 1.0
                    probMat[x][y] = 1.0;


                    // HEY DIPSHIT IF WE ENCOUNTER A HIT THAT HAS BEEN UNREGISTERED PREVIOUSLY WE CAN JUST BREAK AND RETURN SINCE WE ALREADY HAVE THE VALUES WE SHOULD TARGET

                    // set the hitAdjacent coordinates, ensuring it is in bounds and not to overwrite any hits, misses, or sinks
                    if (game.isInBounds(x+1, y) && probMat[x+1] [y] != -1.0 && probMat[x+1][y] != 1.0 && probMat[x+1][y] != -2.0) {
                        probMat[x+1][y] = 0.75; //placeholder values
                    }
                    if (game.isInBounds(x-1, y) && probMat[x-1][y] != -1.0 && probMat[x-1][y] != 1.0 && probMat[x-1][y] != -2.0) {
                        probMat[x-1][y] = 0.75;
                    }
                    if (game.isInBounds(x, y+1) && probMat[x][y+1] != -1.0 && probMat[x][y+1] != 1.0 && probMat[x][y+1] != -2.0) {
                        probMat[x][y+1] = 0.75;
                    }
                    if (game.isInBounds(x, y-1) && probMat[x][y-1] != -1.0 && probMat[x][y-1] != 1.0 && probMat[x][y-1] != -2.0) {
                        probMat[x][y-1] = 0.75;
                    }
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("MISS")) {
                    // set the value of a miss to -1.0
                    probMat[x][y] = -1.0;
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("SUNK")) {
                    // set the value of a sink to -2.0
                    probMat[x][y] = -2.0;
                }
                // if the value has not been previously changed by the hitAdjacent logic then update to unknown value for that coordinate
                else if (probMat[x][y] == 0.0) {
                    // gonna want to make this have a call to another function that handles all the logic of what could be at this coordinate, hopefully wont be too complex (i jinxed it ik)

                    probMat[x][y] = (double)remainingShipCoords / (double)unknown;
                }
            }
        }
        // return the completed probability matrix
        return probMat;
    }




    public Coordinate nextShot(final GameView game, double[][] probMat, ArrayList<Integer[]> adjacents) {
        // init the max value that has been see so far
        double bestProb = -1.0;
        Coordinate bestShot = new Coordinate(100, 100);

        // if there are no hitAdj coords then dont do shit
        if (adjacents.size() != 0) {
            // will pick the proability with the highest chance of being a hit
            for (Integer[] x : adjacents) {
                if (probMat[x[0]][x[1]] > bestProb) {
                    bestProb = probMat[x[0]][x[1]];
                    bestShot = new Coordinate(x[0], x[1]);
                }
            }
        }
        return bestShot;
    }

    


    public ArrayList<Integer[]> visRep(final GameView game,  double[][] probMatrix) {
        ArrayList<Integer[]> close = new ArrayList<>();

        // nested for loop that will go over the probabilities as they are generated from probabilityMatrix for ease of viewing (can switch out to exact prob values)
        for(int y = 0; y < probMatrix.length; y++) {
            for(int x = 0; x < probMatrix[0].length; x++) {
                // a probability of 1.0 in a coordinate denotes a hit 
                if (probMatrix[x][y] == 1.0) {
                    System.out.print("|  " + "HIT" + "  |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -1.0 in a coordinate denotes a miss
                else if (probMatrix[x][y] == -1.0) {
                    System.out.print("| " + "MISS " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -2.0 in a coordinate denotes a sink
                else if (probMatrix[x][y] == -2.0) {
                    System.out.print("| " + "SUNK " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability between 0.5 and 1.0 in a coordinate denotes a hit adjacent
                else if (probMatrix[x][y] >= 0.5 && probMatrix[x][y] < 1.0) {
                    System.out.print("| " + "**** " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                    close.add(new Integer[] {x, y});
                }
                // otherwise we have not shot at that coordinate
                else {
                    System.out.print("| " + "UNKW " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
            }
            System.out.println();
        }
        // returns the arraylist of values that are hit adjacent
        return close;
    }

    
    
    
    public int[] shotTrack(final GameView game) {
        // init tracking variables
        int numHits = 0, numMiss = 0, numSunk = 0, numUnkw = 0;
        
        // determines the hits, misses, sinks, and untargeted shots made by our agent
        for(int y = 0; y < game.getGameConstants().getNumCols(); y++) {
            for(int x = 0; x < game.getGameConstants().getNumRows(); x++) {
                if (game.getEnemyBoardView()[x][y].toString().equals("HIT")) {
                    // incriment the number of hits
                    numHits += 1;  
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("MISS")) {
                    // incriment the number of misses
                    numMiss += 1;
                }
                else if (game.getEnemyBoardView()[x][y].toString().equals("SUNK")) {
                    // incriment the number of sinks (coordinates not actual ships)
                    numSunk += 1;
                }
                else {
                    // if not any of the others than incriment the number of unknown
                    numUnkw += 1;
                }
            }
        }
        // collect the variables and return them in an array
        int[] vals = {numHits, numMiss, numSunk, numUnkw};
        return vals;
    }




    // ignore this 
    @Override
    public void afterGameEnds(final GameView game) {}
}
