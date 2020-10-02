package training.dynamite;

import com.softwire.dynamite.bot.Bot;
import com.softwire.dynamite.game.*;

import java.util.Random;

public class MyBot implements Bot {
    Round lastFinishedRound = null;
    Move enemyLastMove = null;
    Move myLastMove = null;
    Move myMoveToDo = Move.P;
    String lastRoundWinner = "";
    int roundsDone = 0;
    int numberOfRoundsWithMoreAtStakeDone = 0;
    int lastRoundWorth = 0;
    int currentRoundWorth = 1;
    int myScore = 0;
    int enemyScore = 0;
    double percentageOfEnemyDynamiteInHighStakes = 0.0;
    double percentageOfEnemyWaterInHighStakes = 0.0;

    int myDynamitesUsed = 0;
    int enemyDynamitesUsed = 0;

    double[] myMoveProbDWRPS = {0.0001, 0.0002, 33, 66};
    Move[] enemyLast4MovesInHigherStakes = new Move[4];


    public MyBot() {
        // Are you debugging? // Put a breakpoint on the line below to see when we start a new match
        System.out.println("Started new match");
    }

    @Override
    public Move makeMove(Gamestate gamestate) {
        // Are you debugging?  Put a breakpoint in this method to see when we make a move
        if (gamestate.getRounds().size() == 0) return myMoveToDo;
        //update DATA
        updateDataFromPreviousRound(gamestate);
        //SET %
        adjustMoveProbabilities();
        //PICK M
        selectMoveFromProbabilities();

        //PRINT
        if (printConditionIsTrue()) printDataFromPreviousRound();
        if (printConditionIsTrue()) printProbabilities();
        //if (printConditionIsTrue()) System.out.println("My move to do: " + myMoveToDo.name().toString());
        return myMoveToDo;
    }


    private void adjustMoveProbabilities() {
        //adjust water
        if (enemyDynamitesUsed >= 100) {
            setDWProbabilitiesRestDistribute(myMoveProbDWRPS[0], 0.0);
        }

        //SET DYNAMITE
        double waterProbAmount = myMoveProbDWRPS[1] - myMoveProbDWRPS[0];
        if (myDynamitesUsed >= 99) {
            setDWProbabilitiesRestDistribute(0.0, waterProbAmount);
        } else if (currentRoundWorth == 1) {
            setDWProbabilitiesRestDistribute(0.0001, waterProbAmount);
        } else { //if MORE AT STAKE
            double roundsToGo = 2 * (1000 - myScore);
            double roundValue = 3.5 * (currentRoundWorth - 1); //or 4 instead of 3
            double dynamitesIhave = 100 - myDynamitesUsed;
            double numberOfRoundsWithMoreAtStake = roundsToGo / roundValue;
            double dProbNow = dynamitesIhave / numberOfRoundsWithMoreAtStake;
            dProbNow *= 100;

            setDWProbabilitiesRestDistribute(dProbNow, waterProbAmount);
        }

        //adjust WATER
        if (enemyDynamitesUsed < 100 && currentRoundWorth > 1 && percentageOfEnemyDynamiteInHighStakes > 45) {
            //adjust dynamite too
            if (percentageOfEnemyDynamiteInHighStakes >80)myMoveProbDWRPS[0]=1;
            else if (percentageOfEnemyDynamiteInHighStakes >60 && myMoveProbDWRPS[0]>30)myMoveProbDWRPS[0]=20;
            else if (myMoveProbDWRPS[0]>45)myMoveProbDWRPS[0]=45;
            setDWProbabilitiesRestDistribute(myMoveProbDWRPS[0], percentageOfEnemyDynamiteInHighStakes);
        } else {
            setDWProbabilitiesRestDistribute(myMoveProbDWRPS[0], 0.0);
        }

        //adjust fire based to enemy's water activity in expensite situations
        double waterProbAmountNOW = myMoveProbDWRPS[1] - myMoveProbDWRPS[0];
        if(percentageOfEnemyWaterInHighStakes>=51){
            setDWProbabilitiesRestDistribute(0, 0);
        //}else if(percentageOfEnemyWaterInHighStakes>=70){
        //    setDWProbabilitiesRestDistribute(0, waterProbAmountNOW);
        //}else if(percentageOfEnemyWaterInHighStakes>=30){
        //    setDWProbabilitiesRestDistribute(0, 0);
        //}else if(percentageOfEnemyWaterInHighStakes>=15){
        //    setDWProbabilitiesRestDistribute(0, waterProbAmountNOW);
        //}else if(percentageOfEnemyWaterInHighStakes>=90){
        //    setDWProbabilitiesRestDistribute(0, waterProbAmountNOW);
        }

        //adjust D for superCostly situations
        //if (currentRoundWorth>=4){

        //if (printConditionIsTrue()) System.out.println("a "+ currentRoundWorth);
        if (currentRoundWorth>=3){
            //if (printConditionIsTrue()) System.out.println("b");
            if( percentageOfEnemyWaterInHighStakes>=51)setDWProbabilitiesRestDistribute(0,0);
            else if( percentageOfEnemyDynamiteInHighStakes>=51)setDWProbabilitiesRestDistribute(0,100);

            else if( percentageOfEnemyWaterInHighStakes>20)setDWProbabilitiesRestDistribute(0,0);
            else if( percentageOfEnemyDynamiteInHighStakes>20 && myDynamitesUsed<99)setDWProbabilitiesRestDistribute(100,waterProbAmountNOW);
            //else setDWProbabilitiesRestDistribute(100,waterProbAmountNOW);
        }

    }



    private void setDWProbabilitiesRestDistribute(double pD, double pWAmountToAdd) {
        myMoveProbDWRPS[0] = pD;
        myMoveProbDWRPS[1] = pD + pWAmountToAdd;
        double growRate = (100 - myMoveProbDWRPS[1]) / 3;
        myMoveProbDWRPS[2] = myMoveProbDWRPS[1] + growRate;
        myMoveProbDWRPS[3] = myMoveProbDWRPS[2] + growRate;
    }


    private void selectMoveFromProbabilities() {
        double ranD0_100 = new Random().nextDouble() * 100;
        if (ranD0_100 < myMoveProbDWRPS[0]) myMoveToDo = Move.D;
        else if (ranD0_100 < myMoveProbDWRPS[1]) myMoveToDo = Move.W;
        else if (ranD0_100 < myMoveProbDWRPS[2]) myMoveToDo = Move.R;
        else if (ranD0_100 < myMoveProbDWRPS[3]) myMoveToDo = Move.P;
        else myMoveToDo = Move.S;
    }


    private void updateDataFromPreviousRound(Gamestate gamestate) {
        lastFinishedRound = gamestate.getRounds().get(gamestate.getRounds().size() - 1);
        enemyLastMove = lastFinishedRound.getP2();
        myLastMove = lastFinishedRound.getP1();
        if (myLastMove.equals(Move.D)) myDynamitesUsed++;
        if (enemyLastMove.equals(Move.D)) enemyDynamitesUsed++;
        roundsDone = gamestate.getRounds().size();

        updateScoresAndRoundValue();
        updateEnemyLast4MovesInHigherStakes();
    }

    private void updateEnemyLast4MovesInHigherStakes() {
        if (lastRoundWorth > 1) {
            if (enemyLast4MovesInHigherStakes[0] == null) {
                enemyLast4MovesInHigherStakes[0] = enemyLastMove;
            } else if (enemyLast4MovesInHigherStakes[1] == null) {
                enemyLast4MovesInHigherStakes[1] = enemyLastMove;
            } else if (enemyLast4MovesInHigherStakes[2] == null) {
                enemyLast4MovesInHigherStakes[2] = enemyLastMove;
            } else if (enemyLast4MovesInHigherStakes[3] == null) {
                enemyLast4MovesInHigherStakes[3] = enemyLastMove;
            } else { //all slots alocated
                //update last move
                enemyLast4MovesInHigherStakes[0] = enemyLast4MovesInHigherStakes[1];
                enemyLast4MovesInHigherStakes[1] = enemyLast4MovesInHigherStakes[2];
                enemyLast4MovesInHigherStakes[2] = enemyLast4MovesInHigherStakes[3];
                enemyLast4MovesInHigherStakes[3] = enemyLastMove;

                //get % of playing water
                double numberOfDynamiteMoves = 0;
                double numberOfWaterMoves = 0;
                for (Move m : enemyLast4MovesInHigherStakes) {
                    if (m.equals(Move.D)) numberOfDynamiteMoves++;
                    if (m.equals(Move.W)) numberOfWaterMoves++;
                }
                //if (numberOfDynamiteMoves>0) System.out.println("n.of dynamites in 4: "+ numberOfDynamiteMoves);
                percentageOfEnemyDynamiteInHighStakes = numberOfDynamiteMoves / 4 * 100;
                percentageOfEnemyWaterInHighStakes = numberOfWaterMoves / 4 * 100;
                //if (numberOfDynamiteMoves>0) System.out.println("wetar% before sending: "+ percentageOfDynamiteInHighStakes);

            }


        }
    }

    private void updateScoresAndRoundValue() {
        if (xWinsAgainstY(myLastMove, enemyLastMove)) {
            myScore += currentRoundWorth;
            lastRoundWorth = currentRoundWorth;
            lastRoundWinner = "me";
            currentRoundWorth = 1;
        } else if (xWinsAgainstY(enemyLastMove, myLastMove)) {
            enemyScore += currentRoundWorth;
            lastRoundWorth = currentRoundWorth;
            lastRoundWinner = "opponent";
            currentRoundWorth = 1;
        } else {
            lastRoundWorth = currentRoundWorth;
            lastRoundWinner = "DRAW";
            ++currentRoundWorth;
            if (printConditionIsTrue()) System.out.println("current value just updated a");
            //if (currentRoundWorth>=2 && enemyLastMove.equals(Move.D)) System.out.println("current value just updated b");
        }
        if (currentRoundWorth > 1) numberOfRoundsWithMoreAtStakeDone++;
    }

    private static boolean xWinsAgainstY(Move x, Move y) {
        if (x != Move.R || y != Move.S && y != Move.W) {
            if (x != Move.P || y != Move.R && y != Move.W) {
                if (x == Move.S && (y == Move.P || y == Move.W)) {
                    return true;
                } else if (x != Move.D || y != Move.R && y != Move.P && y != Move.S) {
                    return x == Move.W && y == Move.D;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    //PRINT
    private boolean printConditionIsTrue() {
        //return (currentRoundWorth>=4 && ene);
        //return (currentRoundWorth==20);
        return ( myMoveToDo.equals(Move.D));
        //return ( myScore>=9000);
        //return ( enemyLastMove.equals(Move.W));
        //return ( enemyLastMove.equals(Move.W) && currentRoundWorth>1);
        //return ( percentageOfEnemyWaterInHighStakes>0);
        //return ( percentageOfEnemyDynamiteInHighStakes>0);

    }

    private void printDataFromPreviousRound() {
        System.out.println(this.toString());

        //if (enemyLastMove.equals(Move.D)) {
        //    System.out.println("he played dynamite");
        //}

    }

    @Override
    public String toString() {
        return
                //"\n" +
                        //", \n" +
                        "ROUND DATA:" +
                        //", \nroundsDone=" + roundsDone +
                        //", \nnumberOfRoundsWithMoreAtStakeDone=" + numberOfRoundsWithMoreAtStakeDone +
                        //", \ncurrentRoundWORTH=" + currentRoundWorth +
                        //"\n" +
                        //", \nenemyLastMove=" + enemyLastMove +
                        //", \nmyLastMove=" + myLastMove +
                        //", \nlastRoundWinner=" + lastRoundWinner +
                        //", \nlastRoundWorth=" + lastRoundWorth +
                        //"\n" +
                        ", \nmySCORE=" + myScore +
                        ", \nopponentScore=" + enemyScore +
                        //"\n" +
                        //", \nMyDYNAMITEsUsed=" + myDynamitesUsed +
                        //", \nenemyDynamitesUsed=" + enemyDynamitesUsed +
                        ", \npercentageOfEnemyDynamiteInHighStakes=" + percentageOfEnemyDynamiteInHighStakes +
                        //", \npercentageOfEnemyWaterInHighStakes=" + percentageOfEnemyWaterInHighStakes +
                        "";
    }


    private void printProbabilities() {
        System.out.print("PROBABILITIES: ");
        for (double p : myMoveProbDWRPS) {
            double pToPrint = Math.round(p * 100);
            System.out.print(pToPrint / 100 + ", ");
        }
        System.out.println();
    }


    //TRASH
    private Move getMoveThatBeats(Move theirLastMove) {
        switch (theirLastMove) {
            case R:
                return Move.P;
            case P:
                return Move.S;
            case S:
                return Move.R;
            case D:
                return Move.W;
            case W:
                return Move.R;
            default:
                throw new RuntimeException("Invalid last move from P2");
        }
    }
}
