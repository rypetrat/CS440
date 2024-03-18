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

//suggestions add a list that has what's ben taken out of ships and shits (actually no it was 3 am when i wrote this i'm fuckig retarded)
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


        // calculates the probability of a coordinate containing a hit sqaure
        //System.out.println("coordProb: ");

        
        ArrayList<Integer> perm = coordPermutations(game, new Coordinate(0, 0));


        for (int z : perm) {
            System.out.print(z + " ");
        }


        // for separation spacing
        System.out.println();


        System.out.println();
        // delete this shite later
        // if (1 + 1 == 2) {
        //     return new Coordinate(4, 0);
        // }
        
        

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
                        probMat[x+1][y] = 0.85;
                        //System.out.println(probMat[x+1][y]);
                    }
                    if (game.isInBounds(x-1, y) && probMat[x-1][y] != -1.0 && probMat[x-1][y] != 1.0 && probMat[x-1][y] != -2.0) {
                        probMat[x-1][y] = 0.85;
                        //System.out.println(probMat[x-1][y]);
                    }
                    if (game.isInBounds(x, y+1) && probMat[x][y+1] != -1.0 && probMat[x][y+1] != 1.0 && probMat[x][y+1] != -2.0) {
                        probMat[x][y+1] = 0.85;
                        //System.out.println(probMat[x][y+1]);
                    }
                    if (game.isInBounds(x, y-1) && probMat[x][y-1] != -1.0 && probMat[x][y-1] != 1.0 && probMat[x][y-1] != -2.0) {
                        probMat[x][y-1] = 0.85;
                        //System.out.println(probMat[x][y-1]);
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
                    double slay = 0.0;
                    //System.out.println("hi");
                    ArrayList<Integer> perm = coordPermutations(game, new Coordinate(x, y));

<<<<<<< HEAD
                    for(int z = 0; z < perm.size(); z++) {
                        if(z==0)
                        {
                            slay += (double)perm.get(z) * (1.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==1)
                        {
                            slay += (double)perm.get(z) * (2.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==2)
                        {
                            slay += (double)perm.get(z) * (3.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                        else if(z==3)
                        {
                            slay += (double)perm.get(z) * (4.0 / (double)(game.getGameConstants().getNumRows() * game.getGameConstants().getNumCols()));
                        }
                    }
                    probMat[x][y] = slay;
=======
                    probMat[x][y] = wtf(game);
>>>>>>> eba20f11f8b0f3d745c81350db5ee64280d72274
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

        // pick the highest probability coordinate when no adjacents


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
    //Jood what does unknown coordinate mean

    // this is gonna run like ass but can def be smooted out for better runtime but im on a plane and cant be fucked at the moment (its not as bad as i thought but can still be cleaned up if need be)
    public ArrayList<Integer> coordPermutations(final GameView game, Coordinate coord) {
        // gets the curent coordinates x and y position
        int xPos = coord.getXCoordinate();
        int yPos = coord.getYCoordinate();

        ArrayList<Integer> perms = new ArrayList<Integer>();

        // for each ship size
        for (int size = 2; size <= 5; size++) {
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
                        // if we detect a hit further on (just more good evidence than if we dont know whats goin on) (implementing later)
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
                        // if we detect a hit further on (just more good evidence than if we dont know whats goin on) (implementing later)
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
                        // logic for if we detect a hit further on (just more good evidence than if we dont know whats goin on) (implementing later)
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
                        // logic for if we detect a hit further on (just more good evidence than if we dont know whats goin on) (implementing later)
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

<<<<<<< HEAD
=======
public double wtf(final GameView game) {
    int size=game.getGameConstants().getNumCols()*game.getGameConstants().getNumCols();
    double slay=0.0;
    for(int y = 0; y < game.getGameConstants().getNumCols(); y++) {
        for(int x = 0; x < game.getGameConstants().getNumRows(); x++) {
            ArrayList<Integer> perm = coordPermutations(game, probMatrix, new Coordinate(x, y));
            for(int kingRyan = 0; x <perm.size(); kingRyan++) {
                
                
                if(kingRyan==0 )
                {
                    slay+=perm.get(kingRyan)*1/size;
                }
                else if(kingRyan==1 )
                {
                    slay+=perm.get(kingRyan)*2/size;
                }
                else if(kingRyan==2 )
                {
                    slay+=perm.get(kingRyan)*3/size;
                }

                else if(kingRyan==3 )
                {
                    slay+=perm.get(kingRyan)*4/size;
                }

                

            }

        }
    
    }


return slay;
}
>>>>>>> eba20f11f8b0f3d745c81350db5ee64280d72274


    public ArrayList<Integer[]> visRep(final GameView game,  double[][] probMatrix) {
        ArrayList<Integer[]> close = new ArrayList<>();

        // nested for loop that will go over the probabilities as they are generated from probabilityMatrix for ease of viewing (can switch out to exact prob values)
        for(int y = 0; y < probMatrix.length; y++) {
            for(int x = 0; x < probMatrix[0].length; x++) {
                // a probability of 1.0 in a coordinate denotes a hit 
                if (probMatrix[x][y] == 1.0) {
                    //System.out.print("|  " + "HIT" + "  |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -1.0 in a coordinate denotes a miss
                else if (probMatrix[x][y] == -1.0) {
                    //System.out.print("| " + "MISS " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability of -2.0 in a coordinate denotes a sink
                else if (probMatrix[x][y] == -2.0) {
                    //System.out.print("| " + "SUNK " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
                // a probability between 0.5 and 1.0 in a coordinate denotes a hit adjacent
                else if (probMatrix[x][y] > 0.8 && probMatrix[x][y] < 1.0) {
                    //System.out.print("| " + "**** " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                    close.add(new Integer[] {x, y});
                }
                // otherwise we have not shot at that coordinate
                else {
                    //System.out.print("| " + "UNKW " + " |");
                    System.out.print("| " + String.format("%.3g", probMatrix[x][y]) + " |");
                }
            }
            System.out.println();
        }
        // returns the arraylist of values that are hit adjacent
        return close;
    }

    
    
    //returns the num of hits misses and shits
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
