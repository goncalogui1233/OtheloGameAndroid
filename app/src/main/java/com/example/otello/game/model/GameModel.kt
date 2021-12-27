package com.example.otello.game.model

import androidx.lifecycle.MutableLiveData

object GameModel {

    var board = MutableLiveData<Array<IntArray>>()
    var playerTurn = MutableLiveData<Jogador>()
    var numJogadores = MutableLiveData<ArrayList<Jogador>>(arrayListOf())
    var boardDimensions = MutableLiveData<Int>()
    var playPositions = MutableLiveData<ArrayList<Posicoes>>()
    var occupiedPlaces = MutableLiveData<Int>()
    var playerWinner = MutableLiveData<Jogador>()

    var endGame = MutableLiveData(EndGameStates.PLAYING)
    var bombMove = MutableLiveData(false)
    var changePiecesMove = MutableLiveData(false)

    var changePieceArray = arrayListOf<Posicoes>()

    /**
     * Functions
     */

    /**
     * Função que vira todas as peças possíveis numa columa
     */
    fun flipColumn(copyBoard: Array<IntArray>, line: Int, column: Int, flipTop: Boolean) {
        var lin = line
        while (if (flipTop) lin >= 1 else lin < boardDimensions.value!! - 1) {
            if (flipTop) {
                lin--
            } else {
                lin++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][column] == 0)
                break

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[lin][column] == playerTurn.value?.id) {
                while (if (flipTop) lin < line else lin > line) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipTop) {
                        lin++
                    } else {
                        lin--
                    }
                    copyBoard[lin][column] = playerTurn.value?.id!!
                }
                break
            }
        }
    }

    /**
     * Função que vira todas as peças possíveis numa linha
     */
    fun flipLine(copyBoard: Array<IntArray>, line: Int, column: Int, flipLeft: Boolean) {

        var col = column

        while (if (flipLeft) col >= 1 else col < boardDimensions.value!! - 1) {

            if (flipLeft) {
                col--
            } else {
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[line][col] == 0) {
                break
            }

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[line][col] == playerTurn.value?.id) {
                while (if (flipLeft) col < column else col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipLeft) {
                        col++
                    } else {
                        col--
                    }
                    copyBoard[line][col] = playerTurn.value?.id!!
                }
                break
            }

        }
    }

    /**
     * Função que vira todas as peças possíveis em ambas as diagonais do topo
     */
    fun flipTopDiagonal(copyBoard: Array<IntArray>, line: Int, column: Int, flipDiagonalLeft: Boolean) {
        var lin = line
        var col = column
        //Percorrer o board
        while (if (flipDiagonalLeft) lin >= 1 && col >= 1 else lin >= 1 && col < boardDimensions.value!! - 1) {
            if (flipDiagonalLeft) {
                lin--
                col--
            } else {
                lin--
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][col] == 0)
                break

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[lin][col] == playerTurn.value?.id) {
                while (if (flipDiagonalLeft) lin < line && col < column else lin < line && col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipDiagonalLeft) {
                        lin++
                        col++
                    } else {
                        lin++
                        col--
                    }
                    copyBoard[lin][col] = playerTurn.value?.id!!
                }
                break
            }

        }
    }

    /**
     * Função que vira todas as peças possíveis em ambas as diagonais do fundo
     */
    fun flipDiagonalBottom(copyBoard: Array<IntArray>, line: Int, column: Int, flipDiagonalLeft: Boolean) {
        var lin = line
        var col = column
        while (if (flipDiagonalLeft) lin < boardDimensions.value!! - 1 && col >= 1 else lin < boardDimensions.value!! - 1 && col < boardDimensions.value!! - 1) {
            if (flipDiagonalLeft) {
                lin++
                col--
            } else {
                lin++
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][col] == 0)
                break

            if (copyBoard[lin][col] == playerTurn.value?.id) {
                while (if (flipDiagonalLeft) lin > line && col < column else lin > line && col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipDiagonalLeft) {
                        lin--
                        col++
                    } else {
                        lin--
                        col--
                    }
                    copyBoard[lin][col] = playerTurn.value?.id!!
                }
                break
            }
        }
    }

    /**
     * Função que, dada uma linha, procura um local para colocar uma peça
     */
    fun searchBoardLine(line: Int, column: Int, checkingLeft: Boolean, player : Jogador? = null): Posicoes? {
        var col = column
        val board = board.value!!

        var jogadorAtual : Jogador

        if(player == null) {
            jogadorAtual = playerTurn.value!!
        }
        else {
            jogadorAtual = player
        }


        while (if (checkingLeft) col < (boardDimensions.value!! - 1) else col >= 1) {
            if (checkingLeft) {
                col++
            } else {
                col--
            }

            if (board[line][col] != 0 && board[line][col] == jogadorAtual.id) {
                if (checkingLeft) {
                    if (column - 1 >= 0 && board[line][column - 1] == 0) {
                        return Posicoes(line, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if (column + 1 < boardDimensions.value!! && board[line][column + 1] == 0) {
                        return Posicoes(line, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que, dada uma coluna, procura um local para colocar uma peça
     */
    fun searchBoardColumn(linha: Int, coluna: Int, checkingTop: Boolean, player : Jogador? = null): Posicoes? {
        var pos = linha
        val board = board.value!!

        var jogadorAtual : Jogador

        if(player == null) {
            jogadorAtual = playerTurn.value!!
        }
        else {
            jogadorAtual = player
        }

        while (if (checkingTop) pos < (boardDimensions.value!! - 1) else pos >= 1) {
            if (checkingTop) {
                pos++
            } else {
                pos--
            }

            if (board[pos][coluna] == jogadorAtual.id) {
                if (checkingTop) {
                    if ((linha - 1) >= 0 && board[linha - 1][coluna] == 0) {
                        return Posicoes(linha - 1, coluna)
                    } else {
                        return null
                    }
                } else {
                    if ((linha + 1) < boardDimensions.value!! && board[linha + 1][coluna] == 0) {
                        return Posicoes(linha + 1, coluna)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que verifica o board para ver se é possivel colocar uma peça na diagonal
     * esquerda ou direita no topo da posição
     */
    fun searchBoardDiagonalTop(line: Int, column: Int, checkingTopLeft: Boolean, player : Jogador? = null): Posicoes? {
        var lin = line
        var col = column
        val board = board.value!!

        var jogadorAtual : Jogador

        if(player == null) {
            jogadorAtual = playerTurn.value!!
        }
        else {
            jogadorAtual = player
        }

        while (if (checkingTopLeft) (lin < boardDimensions.value!! - 1) && (col < boardDimensions.value!! - 1) else (lin < boardDimensions.value!! - 1 && col >= 1)) {
            if (checkingTopLeft) {
                col++
            } else {
                col--
            }
            lin++

            if (board[lin][col] == jogadorAtual.id) {
                if (checkingTopLeft) {
                    if ((line - 1) >= 0 && (column - 1) >= 0 && board[line - 1][column - 1] == 0) {
                        return Posicoes(line - 1, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if ((line - 1 > 0 && column + 1 < boardDimensions.value!!) && board[line - 1][column + 1] == 0) {
                        return Posicoes(line - 1, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que verifica o board para ver se é possivel colocar uma peça na diagonal
     * esquerda ou direita no fundo da posição
     */
    fun searchBoardDiagonalBottom(line: Int, column: Int, checkingBottomLeft: Boolean, player : Jogador? = null): Posicoes? {
        var lin = line
        var col = column
        val board = board.value!!

        var jogadorAtual : Jogador

        if(player == null) {
            jogadorAtual = playerTurn.value!!
        }
        else {
            jogadorAtual = player
        }

        while (if (checkingBottomLeft) (lin >= 1 && col < boardDimensions.value!! - 1) else (lin >= 1 && col >= 1)) {
            if (checkingBottomLeft)
                col++
            else
                col--
            lin--

            if (board[lin][col] == jogadorAtual.id) {
                if (checkingBottomLeft) {
                    if ((column - 1 >= 0 && line + 1 < boardDimensions.value!!) && board[line + 1][column - 1] == 0) {
                        return Posicoes(line + 1, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if ((line + 1 < boardDimensions.value!! && column + 1 < boardDimensions.value!!) &&
                            board[line + 1][column + 1] == 0) {
                        return Posicoes(line + 1, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    fun bombMove(board: Array<IntArray>, line: Int, column: Int): Array<IntArray> {
        val copyBoard = board

        //Left
        if (column - 1 >= 0) {
            copyBoard[line][column - 1] = 0
        }

        //Diagonal Top Left
        if (line - 1 >= 0 && column - 1 >= 0) {
            copyBoard[line - 1][column - 1] = 0
        }
        //Top
        if (line - 1 >= 0) {
            copyBoard[line - 1][column] = 0
        }

        //Diagonal Top Right
        if (line - 1 >= 0 && column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line - 1][column + 1] = 0
        }

        //Right
        if (column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line][column + 1] = 0
        }

        //Diagonal Bottom Right
        if (line + 1 < GameModel.boardDimensions.value!! && column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line + 1][column + 1] = 0
        }

        //Bottom
        if (line + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line + 1][column] = 0
        }

        //Diagonal Bottom Left
        if (line + 1 < GameModel.boardDimensions.value!! && column - 1 >= 0) {
            copyBoard[line + 1][column - 1] = 0
        }

        return copyBoard
    }

    fun resetGameModel() {
        numJogadores = MutableLiveData<ArrayList<Jogador>>(arrayListOf())
        playPositions = MutableLiveData<ArrayList<Posicoes>>()
        playerTurn = MutableLiveData<Jogador>()
        changePieceArray = arrayListOf()
        boardDimensions = MutableLiveData<Int>()
        bombMove = MutableLiveData(false)
        changePiecesMove = MutableLiveData(false)
        board = MutableLiveData<Array<IntArray>>()
        endGame = MutableLiveData(EndGameStates.PLAYING)
        playerWinner = MutableLiveData()
    }



}