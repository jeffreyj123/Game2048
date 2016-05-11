package com.example.jeffrey.game_2048;

import com.example.jeffrey.game_2048.boardUI.Game;
import com.example.jeffrey.game_2048.boardUI.GameBoard;

import static com.example.jeffrey.game_2048.GameMain.Side.*;

import java.util.Arrays;

public class GameMain {
    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;
    /** Winning number. */
    public static final int WIN = 2048;
    /** True if game is over */
    public static boolean hasWon = false;
    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** Fragment Game interface. */
    private GameFragment.GameListener mListener;

    /** Can be accessed to see if the tile at row ROW and column COL on the
     *  tilted board has already merged once for the current turn. */
    private boolean[][] mMergeHis = new boolean[SIZE][SIZE];

    /** Represents the board: mBoard[row][col] is the tile value at row ROW,
     *  column COL, or 0 if there is no tile there. */
    private int[][] mBoard = new int[SIZE][SIZE];

    /** The current input source and output sink. */
    private Game mGame;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. Also, the maximum score for
     *  the previous game. */
    private int mScore, mMaxScore, mTempMax;
    /** Number of tiles on the board. */
    private int mCount;

    GameMain(GameBoard board, GameFragment.GameListener listener) {
        mGame = new Game(board, SIZE);
        mListener = listener;
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        hasWon = false;
        mScore = 0;
        mCount = 0;
        mGame.clear();
        mTempMax = mMaxScore;
        mListener.setScore(mScore, mMaxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                mBoard[r][c] = 0;
                mMergeHis[r][c] = false;
            }
        }
    }

    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        int[][] board = new int[SIZE][SIZE];

        if (hasWon) {
            return true;
        }

        // copy board
        for (int row = 0; row < SIZE; row += 1) {
            for (int col = 0; col < SIZE; col += 1) {
                board[row][col] = mBoard[row][col];
            }
        }

        // if board full, check if it can be reduced by tilting
        if (mCount == SQUARES) {
            Side[] sides = {NORTH, EAST, WEST, SOUTH};
            for (Side side: sides) {
                if (tiltBoard(side, false)) {
                    mBoard = board;
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /** Mark end of game on display. */
    void endGame() {
        mGame.endGame();
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        if (mCount == SQUARES) {
            return;
        }

        int[][] emptyTiles = new int[SQUARES - mCount][2];
        int row = 0, tiles = 0, col;
        for (; tiles < emptyTiles.length && row < SIZE; row += 1) {
            for (col = 0; tiles < emptyTiles.length && col < SIZE; col += 1) {
                if (mBoard[row][col] == 0) {
                    emptyTiles[tiles] = new int[] { row, col };
                    tiles += 1;
                }
            }
        }

        mCount += 1;
        int[] newTile = mGame.getRandomTile(emptyTiles.length);
        row = emptyTiles[newTile[1]][0];
        col = emptyTiles[newTile[1]][1];

        int value = newTile[0];
        mGame.addTile(value, row, col);
        mBoard[row][col] = value;
    }

    /** Perform the result of tilting the board toward SIDE.
     *  Returns true iff the tilt changes the board. Reforms the
     *  board only if CHANGETILES is true. */
    boolean tiltBoard(Side side, boolean changeTiles) {
        int[][] board = new int[SIZE][SIZE];
        int[][] boardCopy = new int[SIZE][SIZE];

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                        mBoard[tiltRow(side, r, c)][tiltCol(side, r, c)];
                boardCopy[r][c] = mBoard[r][c];
            }
        }

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                int row = tiltRow(side, r, c), col = tiltCol(side, r, c);
                int value = board[r][c];
                if (value != 0) {
                    int boardRow = newRow(board, r , c);
                    int newRow = tiltRow(side, boardRow, c);
                    int newCol = tiltCol(side, boardRow, c);
                    if (boardRow != r) {
                        board[r][c] = 0;
                        board[boardRow][c] = value;
                    }
                    if (canMergeTile(board, boardRow, c)) {
                        newRow = tiltRow(side, boardRow - 1, c);
                        newCol = tiltCol(side, boardRow - 1, c);
                        int v = value, v2 = 2 * value;
                        if (changeTiles) {
                            mGame.mergeTile(v, v2, row, col, newRow, newCol);
                            mCount -= 1;
                            mScore += v2;
                        }
                        board[boardRow][c] = 0;
                        board[boardRow - 1][c] = v2;
                        mMergeHis[boardRow - 1][c] = true;
                    } else {
                        if (changeTiles) {
                            mGame.moveTile(value, row, col, newRow, newCol);
                        }
                    }
                }
            }
        }

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                mBoard[tiltRow(side, r, c)][tiltCol(side, r, c)]
                        = board[r][c];
                mMergeHis[r][c] = false;
            }
        }
        return !Arrays.deepEquals(boardCopy, mBoard);
    }

    /** Return the row number to which the tile at row R and column C on
     *  board BOARD can next move, assuming the board is oriented on side
     *  NORTH. */
    int newRow(int[][] board, int r, int c) {
        while (r > 0 && board[r - 1][c] == 0) {
            r -= 1;
        }
        return r;
    }

    /** Return whether the the tile at row R and column C on board BOARD
     *  can merge with the tile at row R-1 and column C, assuming the board
     *  is oriented on side NORTH. The tile at row R and column C can not
     *  merge if the tile at row R-1 and Column C has already merged. */
    boolean canMergeTile(int[][] board, int r, int c) {
        return r > 0 && !mMergeHis[r - 1][c] && board[r][c] == board[r - 1][c];
    }

    /** Return the row number on a playing board that corresponds to row ROW
     *  and column COL of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int row, int col) {
        switch (side) {
            case NORTH:
                return row;
            case EAST:
                return col;
            case SOUTH:
                return SIZE - 1 - row;
            case WEST:
                return SIZE - 1 - col;
            default:
                throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  ROW and column COL of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns COL (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns ROW. */
    int tiltCol(Side side, int row, int col) {
        switch (side) {
            case NORTH:
                return col;
            case EAST:
                return SIZE - 1 - row;
            case SOUTH:
                return SIZE - 1 - col;
            case WEST:
                return row;
            default:
                throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
            case "Up":
                return NORTH;
            case "Down":
                return SOUTH;
            case "Left":
                return WEST;
            case "Right":
                return EAST;
            default:
                throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        clear();
        setRandomPiece();

        while (true) {
            setRandomPiece();
            if (gameOver()) {
                endGame();
            }

            GetMove:
            while (true) {
                String key = mListener.readKey();

                switch (key) {
                    case "Up": case "Down": case "Left": case "Right":
                        if (!gameOver() && tiltBoard(keyToSide(key), true)) {
                            break GetMove;
                        }
                        break;
                    case "New Game":
                        clear();
                        return true;
                    case "Quit":
                        return false;
                    default:
                        break;
                }
            }
            if (mScore > mMaxScore) {
                mMaxScore = mScore;
            }
            mListener.setScore(mScore, mTempMax);
            mGame.displayMoves();
        }
    }

    /** Display game changes. */
    public void displayMoves() {
        mGame.displayMoves();
    }

    /** Set score values in game, then display. */
    public void setScore(int score, int maxScore) {
        mScore = score;
        mMaxScore = maxScore;
        scoreUpdate();
    }

    /** Update score values in game and display. */
    public void scoreUpdate() {
        if (mScore > mMaxScore) {
            mMaxScore = mScore;
        }
        mListener.setScore(mScore, mTempMax);
    }

    /** Set tiles to values stored in TILES, an array containing triples { V,
     *  R, C } representing tile VALUE, ROW, and COL, respectively. */
    public void setTiles(int[][] tiles) {
        mCount = tiles.length;
        for (int[] tile : tiles) {
            mBoard[tile[1]][tile[2]] = tile[0];
        }
        mGame.setTiles(tiles);
    }

    /** Return MAXSCORE. */
    public int getMaxScore() {
        return mMaxScore;
    }

    /** Return game state as JSON string, containing TILES and SCORE. */
    public String toJSON() {
        StringBuilder stateJSON = new StringBuilder("{ tiles: ");
        stateJSON.append(mGame.toJSON()).append(", score: ");
        stateJSON.append(mScore).append("}");
        return stateJSON.toString();
    }

}
