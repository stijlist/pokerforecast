class Player < ActiveRecord::Base
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable and :omniauthable
  devise :database_authenticatable, :registerable,
         :recoverable, :rememberable, :trackable, :validatable
  def attending?(poker_night)
    !Attendance.where(player_id: self.id, poker_night_id: poker_night.id).empty?
  end
end
