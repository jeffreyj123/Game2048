package com.example.jeffrey.game_2048;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.jeffrey.game_2048.boardUI.GameBoard;
import com.example.jeffrey.game_2048.boardUI.OnSwipeListener;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class GameFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /** Game container. */
    private GameMain mGame;

    /** Game loop task. */
    private GameLoop mGameLoop;

    /** Detects gestures for listener. */
    private GestureDetector mGdt;

    /** Fragment Game interface. */
    private GameListener mListener;

    /** Sound player. */
    private SoundPoolPlayer mSounds;

    /** Root view. */
    private View rootView;

    public GameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GameFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GameFragment newInstance(String param1, String param2) {
        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSounds = new SoundPoolPlayer(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_game, container, false);
        rootView = view;

        final RelativeLayout labelPanel = (RelativeLayout) view.findViewById
                (R.id.label_panel);
        // set app title view size
        Runnable toRun = new Runnable() {
            @Override
            public void run() {
            ((GameActivity) getActivity()).onLoadView(labelPanel.getHeight());
            }
        };
        labelPanel.post(toRun);

        ScoreView currScoreView = (ScoreView) view.findViewById(R.id.current_score);
        ScoreView bestScoreView = (ScoreView) view.findViewById(R.id.best_score);

        // set ScoreView titles
        currScoreView.setTitle("SCORE");
        bestScoreView.setTitle("BEST");

        mListener = new GameListener();
        mGdt = new GestureDetector(getContext(), mListener);

        // attach detector to GameBoard view
        final GameBoard gameBoard = (GameBoard) view.findViewById(R.id.game_board);
        gameBoard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mGdt.onTouchEvent(event)) {
                    return false;
                }
                return true;
            }
        });

        // init game
        mGame = new GameMain(gameBoard, mListener);

        // start new game on new game button click
        Button button = (Button) view.findViewById(R.id.button_new_game);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.newGame();
            }
        });

        return view;
    }

    /** Start game loop. */
    private void startLoop(boolean toInit) {
        mGameLoop = new GameLoop(toInit);
        mGameLoop.execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        // save game state
        SharedPreferences state = getActivity().getSharedPreferences("STATE",
                0);
        SharedPreferences.Editor editor = state.edit();
        editor.putString("gameState", mGame.toJSON());
        editor.putInt("maxScore", mGame.getMaxScore());

        if (mGameLoop != null) {
            mGameLoop.cancel(true);
            mGameLoop = null;
        }

        // save edits
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        // load previous game state
        SharedPreferences state = getActivity().getSharedPreferences("STATE", 0);
        String stateJSON = state.getString("gameState", "");

        setGameState(stateJSON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSounds.release(); // release audio resources
    }

    /** Set game state from JSON string STATEJSON
     *  @param stateJSON the game state */
    public void setGameState(String stateJSON) {
        if (stateJSON == "") {
            startLoop(true); // start game from scratch
            return;
        }
        try {
            // load string into JSON
            JSONObject state = new JSONObject(stateJSON);
            JSONArray tiles = state.getJSONArray("tiles");

            SharedPreferences preferences = getActivity()
                    .getSharedPreferences("STATE", 0);

            // set score
            int score = state.getInt("score"), maxScore;
            maxScore = preferences.getInt("maxScore", 0);
            mListener.setScore(score, maxScore);
            mGame.setScore(score, maxScore);

            // get tiles from JSON data
            int[][] tileArray = new int[tiles.length()][];
            for (int index = 0; index < tiles.length(); index += 1) {
                JSONObject tile = (JSONObject) tiles.get(index);
                tileArray[index] = new int[] { tile.getInt("value"), tile.getInt("row"),
                                               tile.getInt("col") };
            }

            if (mGameLoop == null) {
                startLoop(false); // start loop from state
            }

            mGame.setTiles(tileArray);
        } catch (Exception e) {
            Log.e("GAMESTATE", "JSON READ FAILED");
        }
    }

    public class GameLoop extends AsyncTask<Void, Void, Void> {
        /** True iff to start game from scratch. */
        private boolean mInit;

        public GameLoop(boolean toInit) {
            super();
            mInit = toInit;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Runnable toRun;
            if (mInit) {
                toRun = new Runnable() {
                    @Override
                    public void run() {
                        mGame.clear();
                        mGame.setRandomPiece();
                        synchronized (this) {
                            this.notify();
                        }
                    }
                };
                synchronized (toRun) {
                    getActivity().runOnUiThread(toRun);
                    try {
                        toRun.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            while (true) {
                toRun = new Runnable() {
                    @Override
                    public void run() {
                        if (mInit) {
                            mGame.setRandomPiece();
                        } else {
                            mInit = true;
                        }
                        if (mGame.gameOver()) {
                            mGame.endGame();
                        }
                        synchronized (this) {
                            this.notify();
                        }
                    }
                };
                synchronized (toRun) {
                    getActivity().runOnUiThread(toRun);
                    try {
                        toRun.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                GetMove:
                while (true) {
                    String key = mListener.readKey();

                    switch (key) {
                        case "Up": case "Down": case "Left": case "Right":
                            if (!mGame.gameOver() && mGame.tiltBoard(mGame.keyToSide(key), true)) {
                                break GetMove;
                            }
                            break;
                        case "New Game":
                            toRun = new Runnable() {
                                @Override
                                public void run() {
                                    mGame.clear();
                                    synchronized (this) {
                                        this.notify();
                                    }
                                }
                            };
                            synchronized (toRun) {
                                getActivity().runOnUiThread(toRun);
                                try {
                                    toRun.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            doInBackground();
                            return null;
                        default:
                            break;
                    }
                }
                toRun = new Runnable() {
                    @Override
                    public void run() {
                        mGame.scoreUpdate();
                        mGame.displayMoves();
                        // play sound
                        mSounds.playShortResource(R.raw.blop);
                        synchronized (this) {
                            this.notify();
                        }
                    }
                };
                synchronized (toRun) {
                    getActivity().runOnUiThread(toRun);
                    try {
                        toRun.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class GameListener extends OnSwipeListener {
        /** Queue of pending key presses. */
        private ArrayBlockingQueue<String> _pendingKeys =
                new ArrayBlockingQueue<>(5);

        /** Response to "New Game" button click. */
        public void newGame() {
            _pendingKeys.offer("New Game");
        }

        /** Return the next key press, waiting for it as necessary. */
        public String readKey() {
            try {
                return _pendingKeys.take();
            } catch (InterruptedException excp) {
                throw new Error("unexpected interrupt");
            }
        }

        /** Set the current score being displayed to SCORE and the current
         *  maximum score to MAXSCORE. */
        public void setScore(int score, int maxScore) {
            ScoreView currScoreView = (ScoreView) rootView.findViewById(R.id.current_score);
            ScoreView bestScoreView = (ScoreView) rootView.findViewById(R.id.best_score);

            currScoreView.setText(Integer.toString(score));
            bestScoreView.setText(Integer.toString(maxScore));
        }

        @Override
        public boolean onSwipe(Direction direction) {
            // convert swipe direction to string
            String directionKey;
            if (direction == Direction.up) {
                directionKey = "Up";
            } else if (direction == Direction.down) {
                directionKey = "Down";
            } else if (direction == Direction.left) {
                directionKey = "Left";
            } else if (direction == Direction.right) {
                directionKey = "Right";
            } else {
                return false;
            }
            _pendingKeys.offer(directionKey);
            return true;
        }
    }

    public class SoundPoolPlayer {
        private SoundPool mShortPlayer= null;
        private HashMap mSounds = new HashMap();

        public SoundPoolPlayer(Context pContext) {
            mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

            // add resource id to map
            mSounds.put(R.raw.blop, this.mShortPlayer.load(pContext, R.raw.blop, 1));
        }

        /** Play sound resource. */
        public void playShortResource(int resource) {
            int soundId = (Integer) mSounds.get(resource);
            mShortPlayer.play(soundId, 0.99f, 0.99f, 0, 0, 1);
        }

        /** Release sounds resources and remove player. */
        public void release() {
            this.mShortPlayer.release();
            this.mShortPlayer = null;
        }
    }

}
