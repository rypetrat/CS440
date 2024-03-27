package src.pas.battleship.agents;

// SYSTEM IMPORTS
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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



    
    // final method called and returns the shot that will be fired
    @Override
    public Coordinate makeMove(final GameView game) {
        // get the number of total remaining ships for each size
        ArrayList<Integer> numShips = this.numShips(game);

        // generate probability matrix based on the current board to make an informed next shot
        double[][] probMatrix = this.probabilitiesMatrix(game, numShips);

        // returns an arraylist of the adjacent coords, also prints a visual representation of the probability matrix
        ArrayList<Integer[]> hitAdj = this.hitAdjCoords(game, probMatrix); 

        // generate the coordinate that should be fired at next
        Coordinate fire = this.nextShot(game, probMatrix, hitAdj);

        return fire;
    }




    // gives the matrix of all probabilities of each coordinate in the map
    public double[][] probabilitiesMatrix(final GameView game, ArrayList<Integer> numShips) {
        // generate the matrix that will hold all the probability values for each coordinate
        double[][] probMat = new double[game.getGameConstants().getNumRows()][game.getGameConstants().getNumCols()];

        // nested for loop that will go and set the values for each coordinate in the matrix
        for(int y = 0; y < game.getGameConstants().getNumCols(); y++) {
            for(int x = 0; x < game.getGameConstants().getNumRows(); x++) {
                if (game.getEnemyBoardView()[x][y].toString().equals("HIT")) {
                    // set the value of a hit coordinate to 1.0
                    probMat[x][y] = 2.0;

                    //if we detect any valid hitadj coords breakout (do later)
                    
                    ArrayList<Double> hProb = hitAdjProb(game, new Coordinate(x, y));

                    // set the hitAdjacent coordinates, ensuring it is in bounds and not to overwrite any hits, misses, or sinks
                    if (game.isInBounds(x+1, y) && probMat[x+1] [y] != -1.0 && probMat[x+1][y] != 2.0 && probMat[x+1][y] != -2.0) {
                        probMat[x+1][y] = 1.0 + hProb.get(0);
                    }
                    if (game.isInBounds(x-1, y) && probMat[x-1][y] != -1.0 && probMat[x-1][y] != 2.0 && probMat[x-1][y] != -2.0) {
                        probMat[x-1][y] = 1.0 + hProb.get(1);
                    }
                    if (game.isInBounds(x, y+1) && probMat[x][y+1] != -1.0 && probMat[x][y+1] != 2.0 && probMat[x][y+1] != -2.0) {
                        probMat[x][y+1] = 1.0 + hProb.get(2);
                    }
                    if (game.isInBounds(x, y-1) && probMat[x][y-1] != -1.0 && probMat[x][y-1] != 2.0 && probMat[x][y-1] != -2.0) {
                        probMat[x][y-1] = 1.0 + hProb.get(3);
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
                    //System.out.print(" (" + x + "," + y + ") ");
                    probMat[x][y] = coordShipProb(game, coordPermutations(game, new Coordinate(x, y)), numShips);
                }
            }
        }
        // return the completed probability matrix
        return probMat;
    }




    // normalizes the probabilities and picks the next possible shot to take
    public Coordinate nextShot(final GameView game, double[][] probMat, ArrayList<Integer[]> adjacents) {
        // init the max value that has been see so far
        double bestProb = -2.0;
        Coordinate bestShot = null;

        // init array that will store values for if we have multiple equal highest probability coordinates
        ArrayList<Coordinate> pickFrom = new ArrayList<Coordinate>();

        // if there are hitAdj coords then pick the one with highest probability
        if (adjacents.size() != 0) {
            // will pick the proability with the highest chance of being a hit
            for (Integer[] x : adjacents) {
                if ((probMat[x[0]][x[1]]) > bestProb) {
                    bestProb = (probMat[x[0]][x[1]]);
                    bestShot = new Coordinate(x[0], x[1]);
                }
            }
        }
        else {
            // pick the highest probability coordinate when no adjacents
            for(int y = 0; y < probMat.length; y++) {
                for(int x = 0; x < probMat[0].length; x++) {
                    if ((probMat[x][y]) > bestProb) {
                        bestProb = (probMat[x][y]);
                        bestShot = new Coordinate(x, y);
                        pickFrom.clear();
                        pickFrom.add(bestShot);
                    }
                    else if ((probMat[x][y]) == bestProb) {
                        pickFrom.add(new Coordinate(x, y));
                    }
                }
            }
        }
        //System.out.println("Best probability shots: " + pickFrom);
        if (pickFrom.size() > 1) {
            Random rand = new Random();
            int randomIndex = rand.nextInt(pickFrom.size());
            bestShot = pickFrom.get(randomIndex);
            //System.out.println("picked: " + bestShot);
        }
        return bestShot;
    }




    // this is gonna run like ass but can def be smooted out for better runtime but im on a plane and cant be fucked at the moment (its not as bad as i thought but can still be cleaned up if need be)
    public ArrayList<Integer> coordPermutations(final GameView game, Coordinate coord) {
        // gets the curent coordinates x and y position
        int xPos = coord.getXCoordinate();
        int yPos = coord.getYCoordinate();

        // create a set that contains all the values for remaining ship lengths
        Set<Integer> remaining = new HashSet<>(remainingShips(game));
        //System.out.println("remaining: " + remaining);

        ArrayList<Integer> perms = new ArrayList<Integer>();
        // for each ship size
        for (int size : remaining) {
            // init total x and y valid coords values
            int totalX = 0;
            int totalY = 0;

            // init min x and y values
            int minX = 0;
            int minY = 0;

            // positve x direction
            int posX = 0;
            for (int a = 1; a < size; a++) {
                if (game.isInBounds(xPos+a, yPos)) {
                    if (game.getEnemyBoardView()[xPos+a][yPos].toString().equals("UNKNOWN")) {
                        // if we detect an unknown coordinate then we add to the range of valid possible coordinates
                        posX += 1;
                        totalX += 1;
                    }
                    else if (game.getEnemyBoardView()[xPos+a][yPos].toString().equals("HIT")) {
                        posX += 1;
                        totalX += 1;
                    }
                    else {
                        // if we detect a MISS or SINK break out
                        break;
                    }
                }
            }
            // negative x direction
            int negX = 0;
            for (int b = 1; b < size; b++) {
                if (game.isInBounds(xPos-b, yPos)) {
                    if (game.getEnemyBoardView()[xPos-b][yPos].toString().equals("UNKNOWN")) {
                        // if we detect an unknown coordinate then we add to the range of valid possible coordinates
                        negX += 1;
                        totalX += 1;
                    }
                    else if (game.getEnemyBoardView()[xPos-b][yPos].toString().equals("HIT")) {
                        negX += 1;
                        totalX += 1;
                    }
                    else {
                        // if we detect a MISS or SINK break out
                        break;
                    }
                }
            }
            // positive y direction
            int posY = 0;
            for (int c = 1; c < size; c++) {
                if (game.isInBounds(xPos, yPos+c)) {
                    if (game.getEnemyBoardView()[xPos][yPos+c].toString().equals("UNKNOWN")) {
                        // if we detect an unknown coordinate then we add to the range of valid possible coordinates
                        posY += 1;
                        totalY += 1;
                    }
                    else if (game.getEnemyBoardView()[xPos][yPos+c].toString().equals("HIT")) {
                        posY += 1;
                        totalY += 1;
                    }
                    else {
                        // if we detect a MISS or SINK break out
                        break;
                    }
                }
            }
            // negative y direction
            int negY = 0;
            for (int d = 1; d < size; d++) {
                if (game.isInBounds(xPos, yPos-d)) {
                    if (game.getEnemyBoardView()[xPos][yPos-d].toString().equals("UNKNOWN")) {
                        // if we detect an unknown coordinate then we add to the range of valid possible coordinates
                        negY += 1;
                        totalY += 1;
                    }
                    else if (game.getEnemyBoardView()[xPos][yPos-d].toString().equals("HIT")) {
                        negY += 1;
                        totalY += 1;
                    }
                    else {
                        // if we detect a MISS or SINK break out
                        break;
                    }
                }
            }
            
            // determines the min side length for x directions
            if (posX > negX) {
                minX = negX;
            }
            else {
                minX = posX;
            }
            // determines the min side length for y direction
            if (posY > negY) {
                minY = negY;
            }
            else {
                minY = posY;
            }

            // determines the number of permutations for x direction
            int xPermutations = 0;
            if (totalX >= size-1) {
                if (size == 5) {
                    xPermutations = (totalX) - 3;
                }
                else if (posX == negX && size == 4 && posX == 2) {
                    xPermutations = 2;
                }
                else {
                    xPermutations = minX + 1;
                }
            }

            // determines the number of permutations for y direction
            int yPermutations = 0;
            if (totalY >= size-1) {
                if (size == 5) {
                    yPermutations = (totalY) - 3;
                }
                else if (posY == negY && size == 4 && posY == 2) {
                    yPermutations = 2;
                }
                else {
                    yPermutations = minY + 1;
                }
            }

            // printout and reset of totalx and totaly
            //System.out.println("Ship size: " + size + "    totalX: " + totalX + "    totalY: " + totalY);
            perms.add(yPermutations + xPermutations);
        }
        return perms;
    }




    // gives the probability of all hit adjacent coordinates
    public ArrayList<Double> hitAdjProb(final GameView game, Coordinate coord) {
        // init total values and the arraylist that will hold each side value
        double total = 0.0;
        double xAxTotal = 0.0;
        double yAxTotal = 0.0;
        ArrayList<Double> adjs = new ArrayList<Double>();

        // init boolean axis for if we find a hit on the x or y axis
        boolean hitOnX = false;
        boolean hitOnY = false;

        // init source x and y coords
        int x = coord.getXCoordinate();
        int y = coord.getYCoordinate();
        
        // finds the largest remaining ship size
        Set<Integer> remaining = new HashSet<>(remainingShips(game));
        int maxSize = 0;
        for (Integer value : remaining) {
            if (value > maxSize) {
                maxSize = value;
            }
        }

        // positive x direction
        int posX = 0;
        for (int x1 = 1; x1 < maxSize; x1++) {
            if (game.isInBounds(x+x1, y) && game.getEnemyBoardView()[x+x1][y].toString().equals("HIT")) {
                hitOnX = true;
            }
            else if (game.isInBounds(x+x1, y) && !game.getEnemyBoardView()[x+x1][y].toString().equals("MISS")) {
                xAxTotal += 1;
                posX += 1;
            }
            else {
                break;
            }
        }
        // negative x direction
        int negX = 0;
        for (int x2 = 1; x2 < maxSize; x2++) {
            if (game.isInBounds(x-x2, y) && game.getEnemyBoardView()[x-x2][y].toString().equals("HIT")) {
                hitOnX = true;
            }
            else if (game.isInBounds(x-x2, y) && !game.getEnemyBoardView()[x-x2][y].toString().equals("MISS")) {
                xAxTotal += 1;
                negX += 1;
            }
            else {
                break;
            }
        }

        int posY = 0;
        int negY = 0;
        if (!hitOnX) {
            // positive y direction
            for (int y1 = 1; y1 < maxSize; y1++) {
                if (game.isInBounds(x, y+y1) && game.getEnemyBoardView()[x][y+y1].toString().equals("HIT")) {
                    hitOnY = true;
                }
                else if (game.isInBounds(x, y+y1) && !game.getEnemyBoardView()[x][y+y1].toString().equals("MISS")) {
                    yAxTotal += 1;
                    posY += 1;
                }
                else {
                    break;
                }
            }
            // negative y direction
            for (int y2 = 1; y2 < maxSize; y2++) {
                if (game.isInBounds(x, y-y2) && game.getEnemyBoardView()[x][y-y2].toString().equals("HIT")) {
                    hitOnY = true;
                }
                else if (game.isInBounds(x, y-y2) && !game.getEnemyBoardView()[x][y-y2].toString().equals("MISS")) {
                    yAxTotal += 1;
                    negY += 1;
                }
                else {
                    break;
                }
            }
        }
        // calculates hitadj coord probabilities based on surrounding hits
        if (hitOnX) {
            // positive x
            if ((double)posX / xAxTotal == 1.0) {
                adjs.add(0.99);
            }
            else if (posX == 0) {
                adjs.add(0.01);
            }
            else {
                adjs.add(posX / xAxTotal);
            }
            // negative x
            if ((double)negX / xAxTotal == 1.0) {
                adjs.add(0.99);
            }
            else if (negX == 0) {
                adjs.add(0.01);
            }
            else {
                adjs.add(negX / xAxTotal);
            }
            // positive y
            adjs.add(0.0);
            // negative y
            adjs.add(0.0);
        }
        else if (hitOnY) {
            // positive x
            adjs.add(0.0);
            // negative x
            adjs.add(0.0);
            // positive y
            if ((double)posY / yAxTotal == 1.0) {
                adjs.add(0.99);
            }
            else if (posY == 0) {
                adjs.add(0.01);
            }
            else {
                adjs.add(posY / yAxTotal);
            }
            // negative y
            if ((double)negY / yAxTotal == 1.0) {
                adjs.add(0.99);
            }
            else if (negY == 0) {
                adjs.add(0.01);
            }
            else {
                adjs.add(negY / yAxTotal);
            }
        }
        else {
            total = xAxTotal + yAxTotal;
            // positive x
            if ((double)posX / total == 1.0) {
                adjs.add(0.99);
            }
            else if (posX == 0) {
                adjs.add(0.0);
            }
            else {
                adjs.add((double)posX / total);
            }
            // negative x
            if ((double)negX / total == 1.0) {
                adjs.add(0.99);
            }
            else if (negX == 0) {
                adjs.add(0.0);
            }
            else {
                adjs.add((double)negX / total);
            }
            // positive y
            if ((double)posY / total == 1.0) {
                adjs.add(0.99);
            }
            else if (posY == 0) {
                adjs.add(0.0);
            }
            else {
                adjs.add((double)posY / total);
            }
            // negative y
            if ((double)negY / total == 1.0) {
                adjs.add(0.99);
            }
            else if (negY == 0) { 
                adjs.add(0.0);
            }
            else {
                adjs.add((double)negY / total);
            }
        }
        // return array of each surrounding coordinates probability of containing a ship
        return adjs;
    }


    

    // gives the actual probability of each square based on the number of permutations for each ship size
    public double coordShipProb(final GameView game, ArrayList<Integer> permutations, ArrayList<Integer> numShips) {
        int offset = 0;
        double val = 0.0;
        // probability for ship size 2
        if (numShips.get(0) != 0) {
            val += (numShips.get(0) * (1.0 * permutations.get(0)));
        }
        else {
            offset += 1;
        }
        // probability for ship size 3
        if (numShips.get(1) != 0) {
            val += (numShips.get(1) * (2.0 * permutations.get(1 - offset)));
        }
        else {
            offset += 1;
        }
        // probability for ship size 4
        if (numShips.get(2) != 0) {
            val += (numShips.get(2) * (3.0 * permutations.get(2 - offset)));
        }
        else {
            offset += 1;
        }
        // probability for ship size 5
        if (numShips.get(3) != 0) {
            val += (numShips.get(3) * (4.0 * permutations.get(3 - offset)));
        }
        else {
            offset += 1;
        }
        // divide by the total number of squares to get final prob of the coordinate
        return (val / (game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
    }




    // returns a set of the remaining ship sizes, NOT TYPES
    public Set<Integer> remainingShips(final GameView game) {
        Set<Integer> remaining = new HashSet<>();
        for (Map.Entry<ShipType, Integer> entry : game.getEnemyShipTypeToNumRemaining().entrySet()) {
            if(entry.getKey().toString().equals("AIRCRAFT_CARRIER") && entry.getValue() != 0) {
                // there is a ship of size 5 remaining
                remaining.add(5);
            }
            else if(entry.getKey().toString().equals("BATTLESHIP") && entry.getValue() != 0) {
                // there is a ship of size 4 remaining
                remaining.add(4);
            }
            else if((entry.getKey().toString().equals("DESTROYER") || entry.getKey().toString().equals("SUBMARINE")) && entry.getValue() != 0) {
                // there is a ship of size 3 remaining
                remaining.add(3);
            }
            else if(entry.getKey().toString().equals("PATROL_BOAT") && entry.getValue() != 0) {
                // there is a ship of size 2 remaining
                remaining.add(2);
            } 
        }
        return remaining;
    }




    // returns a set of the remaining ship sizes, NOT TYPES
    public ArrayList<Integer> numShips(final GameView game) {
        ArrayList<Integer> remaining = new ArrayList<>();
        int size2 = 0;
        int size3 = 0;
        int size4 = 0;
        int size5 = 0;
        for (Map.Entry<ShipType, Integer> entry : game.getEnemyShipTypeToNumRemaining().entrySet()) {
            if(entry.getKey().toString().equals("AIRCRAFT_CARRIER") && entry.getValue() != 0) {
                // there is a ship of size 5 remaining
                size5 = entry.getValue();
            }
            else if(entry.getKey().toString().equals("BATTLESHIP") && entry.getValue() != 0) {
                // there is a ship of size 4 remaining
                size4 = entry.getValue();
            }
            else if((entry.getKey().toString().equals("DESTROYER")) && entry.getValue() != 0) {
                // there is a ship of size 3 remaining
                size3 += entry.getValue();
            }
            else if (entry.getKey().toString().equals("SUBMARINE") && entry.getValue() != 0) {
                size3 += entry.getValue();
            }
            else if(entry.getKey().toString().equals("PATROL_BOAT") && entry.getValue() != 0) {
                // there is a ship of size 2 remaining
                size2 += entry.getValue();
            } 
        }
        remaining.add(size2);
        remaining.add(size3);
        remaining.add(size4);
        remaining.add(size5);
        return remaining;
    }




    // gives the visual representation table and returns all hit adjacent coordinates
    public ArrayList<Integer[]> hitAdjCoords(final GameView game,  double[][] probMatrix) {
        ArrayList<Integer[]> close = new ArrayList<>();
        // init the total values
        // nested for loop that will go over the probabilities as they are generated from probabilityMatrix for ease of viewing (can switch out to exact prob values)
        for(int y = 0; y < probMatrix.length; y++) {
            for(int x = 0; x < probMatrix[0].length; x++) {
                // a probability of 2.0 in a coordinate denotes a hit 
                if (probMatrix[x][y] == 2.0) {
                    //System.out.print("|  " + "HIT" + "  |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -1.0 in a coordinate denotes a miss
                else if (probMatrix[x][y] == -1.0) {
                    //System.out.print("| " + "MISS " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -2.0 in a coordinate denotes a sink
                else if (probMatrix[x][y] == -2.0) {
                    //System.out.print("| " + "SUNK " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability between 1.0 and 1.99 in a coordinate denotes a hit adjacent
                else if (probMatrix[x][y] >= 1.0 && probMatrix[x][y] < 2.0) {
                    //System.out.print("| " + "**** " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + "  |");
                    close.add(new Integer[] {x, y});
                }
                // otherwise we have not shot at that coordinate
                else {
                    //System.out.print("| " + "UNKW " + " |");
                    //System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
            }
            //System.out.println();
        }
        // returns the arraylist of values that are hit adjacent
        return close;
    }




    // ignore this 
    @Override
    public void afterGameEnds(final GameView game) {}
}