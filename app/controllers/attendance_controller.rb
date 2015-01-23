class AttendanceController < ApplicationController
  def create
    @attendance = Attendance.new(attendance_params)
    # TODO: make idempotent
    if @attendance.save!
      flash[:success] = "You're attending."
      redirect_to :back
    else
      flash[:error] = "Something went wrong."
      redirect_to :back
    end
  end

  def destroy
    @attendances = Attendance.where(player_id: params[:player_id],
                                   poker_night_id: params[:poker_night_id])

    @attendances.map(&:destroy!)
    redirect_to :back
  end

  private
    def attendance_params
      params.permit(:player_id, :poker_night_id)
    end
end
