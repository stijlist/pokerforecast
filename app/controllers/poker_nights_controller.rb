class PokerNightsController < ApplicationController
  before_action :set_poker_night, only: [:show, :edit, :update, :destroy]
  before_action :authenticate_player!

  # GET /poker_nights
  # GET /poker_nights.json
  def index
    @poker_nights = PokerNight.all
  end

  # GET /poker_nights/1
  # GET /poker_nights/1.json
  def show
  end

  # GET /poker_nights/new
  def new
    @poker_night = PokerNight.new
  end

  # GET /poker_nights/1/edit
  def edit
  end

  # POST /poker_nights
  # POST /poker_nights.json
  def create
    @poker_night = PokerNight.new(poker_night_params)

    respond_to do |format|
      if @poker_night.save
        format.html { redirect_to @poker_night, notice: 'Poker night was successfully created.' }
        format.json { render :show, status: :created, location: @poker_night }
      else
        format.html { render :new }
        format.json { render json: @poker_night.errors, status: :unprocessable_entity }
      end
    end
  end

  # PATCH/PUT /poker_nights/1
  # PATCH/PUT /poker_nights/1.json
  def update
    respond_to do |format|
      if @poker_night.update(poker_night_params)
        format.html { redirect_to @poker_night, notice: 'Poker night was successfully updated.' }
        format.json { render :show, status: :ok, location: @poker_night }
      else
        format.html { render :edit }
        format.json { render json: @poker_night.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /poker_nights/1
  # DELETE /poker_nights/1.json
  def destroy
    @poker_night.destroy
    respond_to do |format|
      format.html { redirect_to poker_nights_url, notice: 'Poker night was successfully destroyed.' }
      format.json { head :no_content }
    end
  end

  private
    # Use callbacks to share common setup or constraints between actions.
    def set_poker_night
      @poker_night = PokerNight.find(params[:id])
    end

    # Never trust parameters from the scary internet, only allow the white list through.
    def poker_night_params
      params.require(:poker_night).permit(:date)
    end
end
