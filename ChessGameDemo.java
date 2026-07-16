public class ChessGameDemo {

    enum Color {
        WHITE, BLACK
    }

    static class Position {
        int row, col;

        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    abstract static class Piece {

        Color color;
        Position position;

        Piece(Color color, Position position) {
            this.color = color;
            this.position = position;
        }

        abstract boolean canMove(Board board, Position to);

        protected boolean isInsideBoard(Position p) {
            return p.row >= 0 && p.row < 8
                    && p.col >= 0 && p.col < 8;
        }
    }

    static class King extends Piece {

        King(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            int dx = Math.abs(position.row - to.row);
            int dy = Math.abs(position.col - to.col);

            return dx <= 1 && dy <= 1;
        }
    }

    static class Queen extends Piece {

        Queen(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            int dx = Math.abs(position.row - to.row);
            int dy = Math.abs(position.col - to.col);

            return dx == dy
                    || position.row == to.row
                    || position.col == to.col;
        }
    }

    static class Rook extends Piece {

        Rook(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            return position.row == to.row
                    || position.col == to.col;
        }
    }

    static class Bishop extends Piece {

        Bishop(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            return Math.abs(position.row - to.row)
                    == Math.abs(position.col - to.col);
        }
    }

    static class Knight extends Piece {

        Knight(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            int dx = Math.abs(position.row - to.row);
            int dy = Math.abs(position.col - to.col);

            return (dx == 2 && dy == 1)
                    || (dx == 1 && dy == 2);
        }
    }

    static class Pawn extends Piece {

        Pawn(Color color, Position position) {
            super(color, position);
        }

        @Override
        boolean canMove(Board board, Position to) {

            int direction =
                    color == Color.WHITE ? -1 : 1;

            return to.row == position.row + direction
                    && to.col == position.col;
        }
    }

    static class Board {

        Piece[][] grid;

        Board() {

            grid = new Piece[8][8];

            initialize();
        }

        private void initialize() {

            // Kings
            grid[0][4] =
                    new King(
                            Color.BLACK,
                            new Position(0, 4)
                    );

            grid[7][4] =
                    new King(
                            Color.WHITE,
                            new Position(7, 4)
                    );

            // Queens
            grid[0][3] =
                    new Queen(
                            Color.BLACK,
                            new Position(0, 3)
                    );

            grid[7][3] =
                    new Queen(
                            Color.WHITE,
                            new Position(7, 3)
                    );

            // Pawns
            for (int col = 0; col < 8; col++) {

                grid[1][col] =
                        new Pawn(
                                Color.BLACK,
                                new Position(1, col)
                        );

                grid[6][col] =
                        new Pawn(
                                Color.WHITE,
                                new Position(6, col)
                        );
            }
        }

        Piece getPiece(Position p) {
            return grid[p.row][p.col];
        }

        void move(Position from, Position to) {

            Piece piece = grid[from.row][from.col];

            grid[to.row][to.col] = piece;

            grid[from.row][from.col] = null;

            piece.position = to;
        }
    }

    static class Player {

        String name;
        Color color;

        Player(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    static class Game {

        Board board;

        Player white;
        Player black;

        Player currentPlayer;

        Game(Player white, Player black) {

            this.white = white;
            this.black = black;

            currentPlayer = white;

            board = new Board();
        }

        public boolean makeMove(
                Position from,
                Position to
        ) {

            Piece piece = board.getPiece(from);

            if (piece == null) {

                System.out.println(
                        "No piece present."
                );

                return false;
            }

            if (piece.color != currentPlayer.color) {

                System.out.println(
                        "Wrong turn."
                );

                return false;
            }

            if (!piece.canMove(board, to)) {

                System.out.println(
                        "Invalid move."
                );

                return false;
            }

            board.move(from, to);

            switchTurn();

            return true;
        }

        private void switchTurn() {

            currentPlayer =
                    currentPlayer == white
                            ? black
                            : white;
        }
    }

    public static void main(String[] args) {

        Player white =
                new Player(
                        "Ayushi",
                        Color.WHITE
                );

        Player black =
                new Player(
                        "Bob",
                        Color.BLACK
                );

        Game game = new Game(
                white,
                black
        );

        game.makeMove(
                new Position(6, 0),
                new Position(5, 0)
        );

        game.makeMove(
                new Position(1, 0),
                new Position(2, 0)
        );
    }
}