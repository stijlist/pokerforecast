class WelcomeController < ApplicationController
  before_action :authenticate_player!

  def index
    if current_player 
      redirect_to poker_nights_path
    else
      redirect_to new_player_path 
    end
  end
end
