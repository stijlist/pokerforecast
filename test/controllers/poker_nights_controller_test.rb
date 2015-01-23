require 'test_helper'

class PokerNightsControllerTest < ActionController::TestCase
  setup do
    @poker_night = poker_nights(:one)
  end

  test "should get index" do
    get :index
    assert_response :success
    assert_not_nil assigns(:poker_nights)
  end

  test "should get new" do
    get :new
    assert_response :success
  end

  test "should create poker_night" do
    assert_difference('PokerNight.count') do
      post :create, poker_night: { date: @poker_night.date }
    end

    assert_redirected_to poker_night_path(assigns(:poker_night))
  end

  test "should show poker_night" do
    get :show, id: @poker_night
    assert_response :success
  end

  test "should get edit" do
    get :edit, id: @poker_night
    assert_response :success
  end

  test "should update poker_night" do
    patch :update, id: @poker_night, poker_night: { date: @poker_night.date }
    assert_redirected_to poker_night_path(assigns(:poker_night))
  end

  test "should destroy poker_night" do
    assert_difference('PokerNight.count', -1) do
      delete :destroy, id: @poker_night
    end

    assert_redirected_to poker_nights_path
  end
end
