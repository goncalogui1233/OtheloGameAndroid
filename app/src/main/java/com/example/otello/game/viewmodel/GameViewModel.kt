package com.example.otello.game.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.otello.game.model.*
import com.example.otello.game.repository.GameRepository

class GameViewModel : ViewModel(){
    
    private val gameModel = GameRepository

    fun initBoard() {
        //Setup Players
        for(i in 0 until 2) {
            gameModel.numJogadores.value?.add(Jogador(i+1))
        }

        //Inicia o Board com todas as posições vazias e guarda o num de colunas e linhas
        gameModel.board.value = Array(8) { IntArray(8)}
        gameModel.boardDimensions.value = 8

        //Organize board when numJogadores = 2
        if(gameModel.numJogadores.value?.size == 2) {
            gameModel.board.value!![3][3] = 1
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![4][3] = 2
            gameModel.board.value!![4][4] = 1
        }

        alterarPontuacoes(gameModel.board.value!!)

        gameModel.gameState.postValue(GameStates.Playing)
    }

    /**
     * Função que insere uma nova peça no tabuleiro
     */
    fun updateValue(line : Int, column : Int) {
        val copyBoard = gameModel.board.value

        if(copyBoard != null && copyBoard[line][column] == 0){
            //Check if it's possible to put piece in that position
            if(gameModel.checkIfPossible(line, column)) {
                //Coloca posição no board
                copyBoard[line][column] = gameModel.playerTurn.value?.id!!

                //Ver todas as peças e muda-las
                val newBoard : Array<IntArray>
                if(gameModel.bombMove.value!!){
                    newBoard = bombMove(copyBoard, line, column)
                    gameModel.playerTurn.value?.bombPiece = false
                    gameModel.bombMove.value = false
                }
                else{
                    newBoard = changePieces(line, column, copyBoard)
                }

                //Alterar as pontuações dos jogadores
                alterarPontuacoes(newBoard)

                //Atualizar o board
                gameModel.board.value = newBoard

                //Verificar se podemos continuar o jogo
                estadoJogo(newBoard)

                //Mudar de jogador
                changePlayer()

            }
        }
    }

    fun bombMove(board: Array<IntArray>, line : Int, column: Int) : Array<IntArray>{
        val copyBoard = board

        //Left
        if(column - 1 >= 0){
            copyBoard[line][column-1] = 0
        }

        //Diagonal Top Left
        if(line - 1 >= 0 && column-1 >= 0){
            copyBoard[line-1][column-1] = 0
        }
        //Top
        if(line - 1 >= 0){
            copyBoard[line-1][column] = 0
        }

        //Diagonal Top Right
        if(line - 1 >= 0 && column + 1 < GameRepository.boardDimensions.value!!){
            copyBoard[line-1][column+1] = 0
        }

        //Right
        if(column + 1 < GameRepository.boardDimensions.value!!){
            copyBoard[line][column+1] = 0
        }

        //Diagonal Bottom Right
        if(line + 1 < GameRepository.boardDimensions.value!! && column + 1 < GameRepository.boardDimensions.value!!){
            copyBoard[line+1][column+1] = 0
        }

        //Bottom
        if(line + 1 < GameRepository.boardDimensions.value!!){
            copyBoard[line+1][column] = 0
        }

        //Diagonal Bottom Left
        if(line + 1 < GameRepository.boardDimensions.value!! && column - 1 >= 0){
            copyBoard[line+1][column-1] = 0
        }

        //Desliga o special no modelo do jogo
        GameRepository.bombMove.value = false

        return copyBoard
    }

    /**
     * Applies special change piece to the board
     * Position 0 & 1 -> Pieces from the current player
     * Position 2 -> Piece from the other player
     */
    fun changePieceMove(){
        val copyBoard = GameRepository.board.value!!
        val changePositionsArray = GameRepository.changePieceArray.value

        if(changePositionsArray != null) {
            val currPlayerPiece = copyBoard[changePositionsArray[0].linha][changePositionsArray[0].coluna]
            val otherPlayerPiece = copyBoard[changePositionsArray[2].linha][changePositionsArray[2].coluna]

            //Altera as peças no board
            copyBoard[changePositionsArray[0].linha][changePositionsArray[0].coluna] = otherPlayerPiece
            copyBoard[changePositionsArray[1].linha][changePositionsArray[1].coluna] = otherPlayerPiece
            copyBoard[changePositionsArray[2].linha][changePositionsArray[2].coluna] = currPlayerPiece

            //Altera a propriedade para o jogador não poder usar este special
            GameRepository.playerTurn.value?.pieceChange = false
            //Desliga o special no jogo
            GameRepository.changePiecesMove.value = false

            //Alterar as pontuações dos jogadores
            alterarPontuacoes(copyBoard)

            //Verificar se podemos continuar o jogo
            estadoJogo(copyBoard)

            //Altera o board
            gameModel.board.value = copyBoard

            //Mudar de jogador
            changePlayer()

        }
    }

    fun estadoJogo(board: Array<IntArray>){
        for(i in 0 until gameModel.boardDimensions.value!!){
            for(j in 0 until gameModel.boardDimensions.value!!){
                if(board[i][j] == 0){
                    return
                }
            }
        }
        gameModel.gameState.postValue(GameStates.EndGame(gameModel.calculateWinner()))
    }

    /**
     * Após um jogador inserir uma peça no tabuleiro, esta função é chamada para
     * alterar as peças já existentes no tabuleiro
     */
    fun changePieces(line: Int, column: Int, copyBoard : Array<IntArray>) : Array<IntArray>{

        //Check Left
        gameModel.flipLine(copyBoard, line, column, true)

        //Check Top Left Diagonal
        gameModel.flipTopDiagonal(copyBoard, line, column, true)

        //Check Top
        gameModel.flipColumn(copyBoard, line, column, true)

        //check top right diagonal
        gameModel.flipTopDiagonal(copyBoard, line, column, false)

        //Check Right
        gameModel.flipLine(copyBoard, line, column, false)

        //Check Bottom Right Diagonal
        gameModel.flipDiagonalBottom(copyBoard, line, column, false)

        //Check Bottom
        gameModel.flipColumn(copyBoard, line, column, false)

        //check bottom left diagonal
        gameModel.flipDiagonalBottom(copyBoard, line, column, true)

        return copyBoard
    }

    /**
     * Esta função percorre o board para contar o numero de peças de cada jogador
     */
    fun alterarPontuacoes(board : Array<IntArray>){
        val pont = arrayListOf(0,0,0)

        for (i in 0 until gameModel.boardDimensions.value!!) {
            for (j in 0 until gameModel.boardDimensions.value!!) {
                when (board[i][j]) {
                    1 -> pont[0]++
                    2 -> pont[1]++
                    3 -> pont[2]++
                }
            }
        }

        for(i in 0 until gameModel.numJogadores.value!!.size){
            gameModel.numJogadores.value!![i].score = pont[i]
        }

        gameModel.currentScores.postValue(pont)
    }

    /**
     * Esta função altera o jogador atual e vê quais os locais onde ele pode jogar.
     */
    fun changePlayer(player: Int = -1) {
        if (player == -1) {
            if (gameModel.playerTurn.value?.id!! == gameModel.numJogadores.value?.size!!) {
                gameModel.playerTurn.value = gameModel.numJogadores.value?.get(0)
            } else {
                gameModel.playerTurn.value = gameModel.numJogadores.
                value?.get(gameModel.numJogadores.value?.indexOf(gameModel.playerTurn.value!!)?.plus(1)!!)
            }
        } else {
            gameModel.playerTurn.value = gameModel.numJogadores.value?.get(player-1)
        }

        getPossiblePositions()
    }

    /**
     * Baseado no jogador atual, esta função procura um local para o jogador jogar
     */
    private fun getPossiblePositions(){
        val board = gameModel.board.value

        if (board != null) {
            val k = arrayListOf<Posicoes>()

            for (i in 0 until gameModel.boardDimensions.value!!) {
                for (j in 0 until gameModel.boardDimensions.value!!) {
                    if (board[i][j] != 0 && board[i][j] != gameModel.playerTurn.value?.id) {

                        var pos: Posicoes? = null
                        //Check Left
                        pos = gameModel.searchBoardLine(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Left
                        pos = gameModel.searchBoardDiagonalTop(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Top
                        pos = gameModel.searchBoardColumn(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Right
                        pos = gameModel.searchBoardDiagonalTop(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Right
                        pos = gameModel.searchBoardLine(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Right
                        pos = gameModel.searchBoardDiagonalBottom(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Bottom
                        pos = gameModel.searchBoardColumn(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Left
                        pos = gameModel.searchBoardDiagonalBottom(i, j, true)
                        if (pos != null)
                            k.add(pos)
                    }
                }
            }
            gameModel.playPositions.value = k
        }
    }

    /**
     * This method calculates clicked position and checks if the piece is to add to the board
     * or if it is to add to the array related to change piece special.
     */
    fun insertPieceOnBoard(position : Int) {
        val linha = position / gameModel.boardDimensions.value!!
        val coluna = position.rem(gameModel.boardDimensions.value!!)

        if(gameModel.changePiecesMove.value!!){
            addChangePiecePosition(Posicoes(linha, coluna))
        }
        else {
            updateValue(linha, coluna)
        }
    }

    private fun addChangePiecePosition(posicoes: Posicoes) {
        val arrPosicoes = gameModel.changePieceArray.value
        arrPosicoes?.add(posicoes)
        gameModel.changePieceArray.postValue(arrPosicoes)
    }

    fun clearPieceChangeArray() {
        gameModel.changePieceArray.value?.clear()
    }

    fun changeBombMoveState() {
        //Só ativa o special da bomba caso outro special não esteja ativo
        if(gameModel.changePiecesMove.value == false) {
            gameModel.bombMove.value?.let {
                gameModel.bombMove.value = !it
            }
        }
    }

    fun changePieceMoveState() {
        if (gameModel.bombMove.value == false) {
            gameModel.changePiecesMove.value?.let {
                gameModel.changePiecesMove.value = !it
            }
        }
    }

    fun changeSeeMovesState() {
        gameModel.seePossibleMoves.value?.let {
            gameModel.seePossibleMoves.value = !it
        }
        gameModel.playPositions.postValue(gameModel.playPositions.value)
    }

    /**
     * This method changes the player's moves flag to false to indicate that
     * in the last turn, he didn't have any moves to make
     */
    fun setPlayerNoPossibleMoves() {
        gameModel.playerTurn.value?.let {
            it.hadMoves = false
        }
        gameModel.checkPlayerMoves()
    }

    /**
     * Game getters
     */

    fun getBoardLiveData() : LiveData<Array<IntArray>> = gameModel.board

    fun getBombActionStateLiveData() : LiveData<Boolean> = gameModel.bombMove

    fun getPieceChangeActionStateLiveData() : LiveData<Boolean> = gameModel.changePiecesMove

    fun getSeeMovesStateLiveData() : LiveData<Boolean> = gameModel.seePossibleMoves

    fun getGameStateLiveData() : LiveData<GameStates> = gameModel.gameState

    fun getPlayerTurnStateLiveData(): LiveData<Jogador> = gameModel.playerTurn

    fun getCurrPlayerPlayPositionsLiveData(): LiveData<ArrayList<Posicoes>> = gameModel.playPositions

    fun getScoresLiveData(): LiveData<ArrayList<Int>> = gameModel.currentScores

    fun getChangePieceArrayLiveData(): LiveData<ArrayList<Posicoes>> = gameModel.changePieceArray


}